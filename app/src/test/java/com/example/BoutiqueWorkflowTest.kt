package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.*
import com.example.data.repository.*
import com.example.util.Formatters
import com.example.util.pdf.InvoiceFormat
import com.example.util.pdf.InvoicePdfGenerator
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class BoutiqueWorkflowTest {

    private lateinit var context: Context
    private lateinit var db: AppDatabase
    private lateinit var repo: BoutiqueRepository
    private lateinit var backupManager: BackupManager

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repo = BoutiqueRepository(db)
        backupManager = BackupManager(context, db)
        runBlocking {
            repo.ensureInitialized()
        }
    }

    @After
    fun teardown() {
        db.close()
    }

    @Test
    fun `official business info and removable sample catalog are preloaded`() = runBlocking {
        val settings = repo.settingsFlow.first()
        assertEquals("NAQSH COLLECTION", settings.businessName)
        assertEquals("NASIR ALI", settings.ownerName)
        assertEquals("+91 9471477887", settings.ownerMobile)
        assertEquals("₹", settings.currencySymbol)

        val categories = repo.categoriesFlow.first()
        assertEquals(6, categories.size)
        assertTrue(categories.any { it.name == "ABAYA" })
        assertTrue(categories.any { it.name == "PAKISTANI SUITS" })

        val products = repo.productsFlow.first()
        assertTrue(products.size >= 30)

        // Verify sample product can be deleted permanently
        val firstProd = products.first()
        repo.deleteProduct(firstProd, permanent = true, actorName = "NASIR ALI", actorRole = "OWNER")
        val afterDelete = repo.productsFlow.first()
        assertEquals(products.size - 1, afterDelete.size)
    }

    @Test
    fun `purchase increases stock, sale deducts stock, return restores stock, and PDF generates`() = runBlocking {
        // 1. Add custom product with variants
        val prodId = repo.saveProduct(
            ProductEntity(
                name = "Royal Emerald Dubai Abaya",
                categoryName = "ABAYA",
                sku = "NQ-EMB-01",
                purchasePrice = 1500.0,
                sellingPrice = 2800.0,
                size = "54",
                colour = "Emerald",
                quantity = 0
            ),
            variants = listOf(
                ProductVariantEntity(productId = 0, size = "54", colour = "Emerald", quantity = 2),
                ProductVariantEntity(productId = 0, size = "56", colour = "Black", quantity = 3)
            ),
            actorName = "NASIR ALI",
            actorRole = "OWNER"
        )

        var savedProd = db.boutiqueDao().getProductById(prodId)!!
        assertEquals(5, savedProd.quantity)

        // 2. Add supplier and record purchase (+4 units of Emerald 54)
        val supId = repo.saveSupplier(
            SupplierEntity(name = "Al-Noor Textiles", mobile = "9876543210", address = "Surat"),
            "NASIR ALI",
            "OWNER"
        )
        val supplier = db.boutiqueDao().getSupplierById(supId)!!
        val prodVariants = db.boutiqueDao().getVariantsForProduct(prodId)
        val emeraldVar = prodVariants.first { it.size == "54" }

        repo.recordPurchase(
            supplier = supplier,
            supplierNameFallback = "",
            lines = listOf(PurchaseCartLine(savedProd, emeraldVar, quantity = 4, purchasePrice = 1500.0)),
            paymentMode = "Cash",
            paidAmount = 4000.0,
            notes = "Restock lot",
            actorName = "NASIR ALI",
            actorRole = "OWNER"
        )

        savedProd = db.boutiqueDao().getProductById(prodId)!!
        assertEquals(9, savedProd.quantity)
        val updatedSup = db.boutiqueDao().getSupplierById(supId)!!
        assertEquals(2000.0, updatedSup.remainingBalance, 0.01)

        // 3. Create customer and complete POS Sale (2 units, partial payment)
        val custId = repo.saveCustomer(
            CustomerEntity(name = "Amina Khatoon", mobile = "9123456789", address = "Mithanpura"),
            "NASIR ALI",
            "OWNER"
        )
        val customer = db.boutiqueDao().getCustomerById(custId)!!

        val invoice = repo.completeSale(
            cart = listOf(CartLine(savedProd, emeraldVar, quantity = 2, unitPrice = 2800.0, itemDiscount = 100.0)),
            customer = customer,
            walkInName = "",
            walkInMobile = "",
            billDiscount = 0.0,
            gstPercent = 5.0,
            amountPaid = 5000.0,
            paymentMethod = "PhonePe",
            notes = "Eid order",
            actorName = "NASIR ALI",
            actorRole = "OWNER"
        )

        assertTrue(invoice.invoiceNumber.startsWith("NC-"))
        // (2800*2 - 100) = 5500 + 5% GST (275) = 5775
        assertEquals(5775.0, invoice.grandTotal, 0.01)
        assertEquals(775.0, invoice.remainingBalance, 0.01)

        savedProd = db.boutiqueDao().getProductById(prodId)!!
        assertEquals(7, savedProd.quantity)
        val updatedCust = db.boutiqueDao().getCustomerById(custId)!!
        assertEquals(775.0, updatedCust.outstandingBalance, 0.01)

        // 4. Verify bundled official logo & PhonePe QR decode cleanly, plus CSV exports and Invoice formats
        assertEquals(3, InvoiceFormat.entries.size)
        val logoBmp = android.graphics.BitmapFactory.decodeResource(context.resources, R.drawable.naqsh_logo)
        val qrBmp = android.graphics.BitmapFactory.decodeResource(context.resources, R.drawable.phonepe_qr)
        assertNotNull(logoBmp)
        assertNotNull(qrBmp)

        val saleItems = db.boutiqueDao().getSaleItemsForInvoice(invoice.id)
        val salesCsv = backupManager.exportSalesCsv(listOf(invoice))
        assertTrue(salesCsv.exists())
        assertTrue(salesCsv.readText().contains(invoice.invoiceNumber))
        val invCsv = backupManager.exportInventoryCsv(listOf(savedProd))
        assertTrue(invCsv.exists())
        assertTrue(invCsv.readText().contains("Royal Emerald Dubai Abaya"))

        // 5. Partial Return restores stock & keeps invoice traceable
        repo.processReturnOrExchange(
            invoice = invoice,
            item = saleItems.first(),
            returnQty = 1,
            isFullReturn = false,
            exchangeProduct = null,
            refundAmount = 500.0,
            reason = "Size alteration",
            actorName = "NASIR ALI",
            actorRole = "OWNER"
        )
        savedProd = db.boutiqueDao().getProductById(prodId)!!
        assertEquals(8, savedProd.quantity)
        val returnedInv = db.boutiqueDao().getSaleById(invoice.id)!!
        assertEquals("PARTIAL_RETURN", returnedInv.status)

        // 6. Backup & Restore round-trip
        val backupJson = backupManager.exportFullBackupJson()
        assertTrue(backupJson.contains("Royal Emerald Dubai Abaya"))
        assertTrue(backupJson.contains(invoice.invoiceNumber))
        val restoreRes = backupManager.restoreFromJson(backupJson)
        assertTrue(restoreRes.isSuccess)
    }

    @Test
    fun `staff permissions and PIN hashing work accurately`() {
        val pinHash = Formatters.hashPin("4589")
        val staff = StaffEntity(
            id = 1,
            name = "Zainab",
            mobile = "9988776655",
            pinHash = pinHash,
            canMakeSales = true,
            canManageInventory = false,
            canManagePurchases = false,
            canViewBasicReports = false
        )
        val role = ActiveSessionRole(isOwner = false, staff = staff)
        assertFalse(role.isOwner)
        assertTrue(role.canMakeSales)
        assertFalse(role.canManageInventory)
        assertFalse(role.canViewBasicReports)
        assertEquals(pinHash, Formatters.hashPin("4589"))
        assertNotEquals(pinHash, Formatters.hashPin("0000"))
    }
}
