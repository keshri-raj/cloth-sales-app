package com.shop.salesapp.ui.theme

import androidx.compose.runtime.Composable

@Composable
fun tr(english: String, hindi: String): String = if (salesAppHindi.value) hindi else english

fun localizedAppMessage(message: String, hindi: Boolean): String {
    if (!hindi) return message
    val translations = mapOf(
        "The request could not be completed. Try again or contact the manager." to "अनुरोध पूरा नहीं हो सका। कृपया फिर कोशिश करें या मैनेजर से बात करें।",
        "Request is empty or too large." to "अनुरोध खाली है या बहुत बड़ा है।",
        "Sales App is not fully configured. Ask the manager to finish setup." to "ऐप का सेटअप पूरा नहीं है। मैनेजर से सेटअप पूरा करने को कहें।",
        "Sign in to use Sales App." to "ऐप इस्तेमाल करने के लिए साइन इन करें।",
        "Your sign-in expired. Sign in again." to "आपका साइन-इन समाप्त हो गया है। फिर से साइन इन करें।",
        "Your sign-in is invalid or expired. Sign in again." to "आपका साइन-इन सही नहीं है या समाप्त हो गया है। फिर से साइन इन करें।",
        "Could not verify your Google sign-in." to "Google साइन-इन की पुष्टि नहीं हो सकी।",
        "Use a verified Google account." to "पुष्टि किया हुआ Google खाता इस्तेमाल करें।",
        "This Google account is not allowed to use Sales App." to "इस Google खाते को ऐप इस्तेमाल करने की अनुमति नहीं है।",
        "Google sign-in completed, but this build needs the server OAuth client ID." to "Google साइन-इन पूरा हुआ, लेकिन ऐप सेटअप अधूरा है। मैनेजर से संपर्क करें।",
        "Could not sign in. Try again or ask the manager to check access." to "साइन-इन नहीं हो सका। फिर कोशिश करें या मैनेजर से अनुमति जाँचने को कहें।",
        "Enter the customer name." to "ग्राहक का नाम लिखें।",
        "Enter a valid customer phone number." to "ग्राहक का सही फ़ोन नंबर लिखें।",
        "Enter a valid phone number first." to "पहले सही फ़ोन नंबर लिखें।",
        "A customer with this phone number already exists." to "इस फ़ोन नंबर वाला ग्राहक पहले से मौजूद है।",
        "That contact has no phone number. You can type it instead." to "इस संपर्क में फ़ोन नंबर नहीं है। नंबर खुद लिखें।",
        "Could not read the selected phone number. You can type it instead." to "संपर्क से फ़ोन नंबर नहीं मिल सका। नंबर खुद लिखें।",
        "Could not search customers." to "ग्राहक खोजे नहीं जा सके। इंटरनेट जाँचकर फिर कोशिश करें।",
        "Could not load this customer's sales." to "इस ग्राहक की बिक्री की जानकारी नहीं मिल सकी।",
        "Could not load this customer's order requests." to "इस ग्राहक के ऑर्डर की जानकारी नहीं मिल सकी।",
        "Sign in to Shop before loading order requests." to "ऑर्डर अनुरोध देखने से पहले Shop में साइन इन करें।",
        "Could not load order requests." to "ऑर्डर अनुरोध लोड नहीं हो सके।",
        "Could not load this customer's payment balance." to "इस ग्राहक का बकाया नहीं मिल सका।",
        "Sign in to Shop before saving a product picture." to "तस्वीर सहेजने से पहले Shop में साइन इन करें।",
        "Shop did not save the product picture. Try again." to "Shop में तस्वीर सहेजी नहीं गई। फिर कोशिश करें।",
        "Product picture saved to Shop." to "कपड़े की तस्वीर Shop में सहेज दी गई।",
        "Could not save this product picture." to "कपड़े की तस्वीर सहेजी नहीं जा सकी।",
        "Could not use this picture." to "यह तस्वीर इस्तेमाल नहीं हो सकी। दूसरी तस्वीर चुनें।",
        "The picture was not captured. Try again." to "तस्वीर नहीं ली जा सकी। फिर कोशिश करें।",
        "Could not open the camera." to "कैमरा नहीं खुल सका। कैमरे की अनुमति जाँचें।",
        "Sign in before syncing this payment. It is saved on this phone." to "भुगतान सिंक करने के लिए साइन इन करें। यह इस फ़ोन में सुरक्षित है।",
        "Payment saved. Shop sync will continue in the background." to "भुगतान सुरक्षित है। Shop में सिंक जारी रहेगा।",
        "Sign in to Shop before sharing this bill." to "बिल साझा करने से पहले Shop में साइन इन करें।",
        "Could not prepare this bill for sharing." to "बिल साझा करने के लिए तैयार नहीं हो सका।",
        "Could not create the QR label PDF." to "QR लेबल PDF नहीं बन सका।",
        "Connect to Shop before sending an order request." to "ऑर्डर भेजने से पहले Shop से जुड़ें।",
        "Enter a valid customer phone number and customer name." to "ग्राहक का सही फ़ोन नंबर और नाम लिखें।",
        "Add at least one cloth before sending the order." to "ऑर्डर भेजने से पहले कम से कम एक कपड़ा जोड़ें।",
        "Could not send the order request." to "ऑर्डर अनुरोध नहीं भेजा जा सका।",
        "Connect to Shop before saving customers." to "ग्राहक सहेजने से पहले Shop से जुड़ें।",
        "Could not save this customer." to "ग्राहक सहेजा नहीं जा सका।",
        "Could not scan. Type the cloth name or code." to "स्कैन नहीं हो सका। कपड़े का नाम या कोड लिखें।",
        "Could not scan. You can search by cloth name." to "स्कैन नहीं हो सका। कपड़े का नाम लिखकर खोजें।",
        "QR scanner could not start. Check your internet connection and Google Play services, then try again." to "QR स्कैनर शुरू नहीं हो सका। इंटरनेट और Google Play सेवाएँ जाँचकर फिर कोशिश करें।",
        "Connect the Shop API and sign in before saving clothes." to "कपड़े सहेजने से पहले Shop से जुड़ें और साइन इन करें।",
        "Could not save this cloth." to "कपड़ा सहेजा नहीं जा सका।",
        "Cloth not found in Shop. Check the QR label." to "Shop में यह कपड़ा नहीं मिला। QR लेबल जाँचें।",
        "Cloth not found in preview catalog. Search by name or code." to "नमूना सूची में कपड़ा नहीं मिला। नाम या कोड से खोजें।",
        "Could not look up this cloth." to "कपड़े की जानकारी नहीं मिल सकी।",
        "Connect the Shop API and sign in before saving bills." to "बिल सहेजने से पहले Shop से जुड़ें और साइन इन करें।",
        "Could not save the bill." to "बिल सहेजा नहीं जा सका।",
        "Sign in to Shop before syncing bills. Your saved bills are still available on this phone." to "बिल सिंक करने के लिए Shop में साइन इन करें। इस फ़ोन में सहेजे बिल उपलब्ध हैं।",
        "Could not sync bills. Saved bills are still available on this phone." to "बिल सिंक नहीं हो सके। इस फ़ोन में सहेजे बिल उपलब्ध हैं।",
        "Sign in to Shop before syncing balances. Saved balances are still available on this phone." to "बकाया सिंक करने के लिए Shop में साइन इन करें। इस फ़ोन में सहेजी जानकारी उपलब्ध है।",
        "Could not sync outstanding balances." to "बकाया सिंक नहीं हो सका।",
        "Could not open the bill PDF." to "बिल PDF नहीं खुल सका।",
        "Could not load clothes from Shop." to "Shop से कपड़ों की सूची नहीं मिल सकी। सिंक करके फिर कोशिश करें।",
        "Sign in to Shop before syncing the catalog." to "कैटलॉग सिंक करने से पहले Shop में साइन इन करें।",
        "Catalog synced from Shop." to "कैटलॉग Shop से सिंक हो गया।",
        "Could not sync the catalog from Shop." to "कैटलॉग Shop से सिंक नहीं हो सका। इंटरनेट जाँचकर फिर कोशिश करें।",
        "The Inventory tab was not found." to "Shop शीट में Inventory टैब नहीं मिला। मैनेजर से संपर्क करें।",
        "Product picture is too large. Choose a smaller picture." to "तस्वीर बहुत बड़ी है। छोटी तस्वीर चुनें।",
        "Could not load the product picture." to "कपड़े की तस्वीर लोड नहीं हो सकी।",
        "Choose a product before adding its picture." to "तस्वीर जोड़ने से पहले कपड़ा चुनें।",
        "Choose a picture before saving." to "सहेजने से पहले तस्वीर चुनें।",
        "This product was not found in Shop. Sync products and try again." to "Shop में यह कपड़ा नहीं मिला। सूची सिंक करके फिर कोशिश करें।",
        "Add between 1 and 100 clothes before sending the order." to "ऑर्डर भेजने के लिए 1 से 100 कपड़े जोड़ें।",
        "Enter the customer name before sending this order." to "ऑर्डर भेजने से पहले ग्राहक का नाम लिखें।",
        "Enter a valid phone number to search bills." to "बिल खोजने के लिए सही फ़ोन नंबर लिखें।",
        "Choose a bill before sharing its PDF." to "PDF साझा करने से पहले बिल चुनें।",
        "This bill was not found in Shop. Sync bills and try again." to "Shop में यह बिल नहीं मिला। बिल सिंक करके फिर कोशिश करें।",
        "The PDF link for this bill is missing. Ask the manager to check the invoice." to "इस बिल की PDF लिंक नहीं मिली। मैनेजर से बिल जाँचने को कहें।",
        "The bill PDF is not available in the private invoice folder." to "बिल PDF निजी फ़ोल्डर में उपलब्ध नहीं है। मैनेजर से संपर्क करें।",
        "Bill items are incomplete, so its PDF cannot be refreshed. Ask the manager to check this bill." to "बिल की जानकारी अधूरी है, इसलिए PDF अपडेट नहीं हुई। मैनेजर से बिल जाँचने को कहें।",
        "This bill PDF is too large to share from the phone." to "बिल PDF फ़ोन से साझा करने के लिए बहुत बड़ी है।",
        "Choose the bill this payment is for." to "भुगतान के लिए बिल चुनें।",
        "Enter a whole-number payment amount greater than zero." to "शून्य से अधिक, पूरे रुपये में भुगतान राशि लिखें।",
        "Choose Cash, UPI, or Card for this payment." to "भुगतान का तरीका Cash, UPI या Card चुनें।",
        "That bill was not found for this customer." to "इस ग्राहक का बिल नहीं मिला।",
        "This bill does not have a valid total." to "इस बिल की कुल राशि सही नहीं है।",
        "This payment reference was already used for a different bill." to "यह भुगतान संदर्भ किसी दूसरे बिल में इस्तेमाल हो चुका है।",
        "This bill is already paid in full." to "इस बिल का पूरा भुगतान हो चुका है।",
        "This payment reference was already used for a different customer." to "यह भुगतान संदर्भ किसी दूसरे ग्राहक में इस्तेमाल हो चुका है।",
        "This customer has no unpaid bills." to "इस ग्राहक का कोई बकाया बिल नहीं है।",
        "The payment reference is invalid. Try again." to "भुगतान संदर्भ सही नहीं है। फिर कोशिश करें।",
        "Enter a product name, valid prices, and a whole-number quantity." to "कपड़े का नाम, सही कीमतें और पूरी संख्या में मात्रा लिखें।",
        "Choose Cash, UPI, Card, or Credit." to "Cash, UPI, Card या उधार चुनें।",
        "Add between 1 and 100 clothes before saving the bill." to "बिल सहेजने से पहले 1 से 100 कपड़े जोड़ें।",
        "One or more bill items are missing a cloth code. Remove the incomplete item and add it again." to "एक या अधिक कपड़ों का कोड नहीं है। अधूरा कपड़ा हटाकर फिर जोड़ें।",
        "This number is new. Enter the customer name." to "यह नया नंबर है। ग्राहक का नाम लिखें।",
        "A selected cloth is missing its name in Inventory. Ask the manager to correct it before billing." to "Inventory में चुने कपड़े का नाम नहीं है। बिल बनाने से पहले मैनेजर से ठीक करवाएँ।",
        "Some bill details are incomplete. Check the cloth, price, and quantity, then try again." to "बिल की कुछ जानकारी अधूरी है। कपड़ा, कीमत और मात्रा जाँचकर फिर कोशिश करें।",
        "The bill total must be greater than zero. Check the item prices and discounts, then try again." to "बिल की कुल राशि शून्य से अधिक होनी चाहिए। कीमत और छूट जाँचकर फिर कोशिश करें।",
        "Discount is too high. The bill total cannot be below the combined purchase cost of the items." to "छूट बहुत ज़्यादा है। बिल की कुल राशि सभी कपड़ों की संयुक्त खरीद कीमत से कम नहीं हो सकती।",
        "Discount cannot be more than the bill subtotal." to "छूट बिल की उप-कुल राशि से अधिक नहीं हो सकती।",
        "Enter a valid bill discount." to "बिल की सही छूट दर्ज करें।",
        "Choose Cash, UPI, or Card for the amount received." to "मिली हुई राशि का तरीका Cash, UPI या Card चुनें।",
        "Another sale is being saved. Please try again in a moment." to "दूसरी बिक्री सहेजी जा रही है। थोड़ी देर में फिर कोशिश करें।",
        "There is not enough room for all bill items. Ask the manager to make space in Sales Items." to "बिल के सभी कपड़ों के लिए जगह नहीं है। मैनेजर से Sales Items में जगह बनाने को कहें।",
        "The Order Requests tab is full. Ask the manager to make space." to "Order Requests टैब भर गया है। मैनेजर से जगह बनाने को कहें।"
    )
    translations[message]?.let { return it }
    return when {
        message.startsWith("That product name already exists. SKU:") -> "यह कपड़ा पहले से मौजूद है। SKU: ${message.substringAfter("SKU:").trim()}"
        message.startsWith("Cloth ") && message.contains(" was not found.") -> "कपड़े का कोड ${message.substringAfter("Cloth ").substringBefore(" was not found.")} नहीं मिला। इसे हटाकर फिर जोड़ें।"
        message.startsWith("Enter a whole-number quantity for ") -> "${message.substringAfter("Enter a whole-number quantity for ").removeSuffix(".")} की मात्रा पूरी संख्या में लिखें।"
        message.startsWith("Only ") && message.contains(" units of ") -> {
            val match = Regex("Only (.+) units of (.+) are in stock\\.").matchEntire(message)
            if (match != null) "${match.groupValues[2]} का स्टॉक केवल ${match.groupValues[1]} है।" else "स्टॉक उपलब्ध नहीं है। मात्रा जाँचें।"
        }
        message.startsWith("A valid selling price is missing for ") -> "${message.substringAfter("A valid selling price is missing for ").substringBefore(". Ask")} की बेचने की कीमत दर्ज नहीं है। मैनेजर से ठीक करवाएँ।"
        message.startsWith("Stock information is incomplete for ") -> "${message.substringAfter("Stock information is incomplete for ").substringBefore(". Ask")} का स्टॉक दर्ज नहीं है। मैनेजर से ठीक करवाएँ।"
        message.startsWith("Cost information is invalid for ") -> "${message.substringAfter("Cost information is invalid for ").substringBefore(". Ask")} की खरीद कीमत सही नहीं है। मैनेजर से ठीक करवाएँ।"
        message.startsWith("Enter a whole-number quantity greater than zero for ") -> "${message.substringAfter("Enter a whole-number quantity greater than zero for ").removeSuffix(".")} की मात्रा शून्य से अधिक पूरी संख्या में लिखें।"
        message.startsWith("Payment is greater than the current due of ₹") -> "भुगतान की राशि मौजूदा बकाया से अधिक है। राशि जाँचें।"
        message.startsWith("Payment is greater than the customer due of ₹") -> "भुगतान की राशि ग्राहक के कुल बकाया से अधिक है। राशि जाँचें।"
        message.startsWith("Amount received must be between ₹") -> "मिली हुई राशि शून्य और बिल की कुल राशि के बीच होनी चाहिए। राशि जाँचें।"
        message.startsWith("Unsupported sheet tab:") -> "Shop शीट में जरूरी टैब नहीं मिला। मैनेजर से संपर्क करें।"
        message.startsWith("Required tab '") || message.startsWith("Tab ") -> "Shop शीट में जरूरी कॉलम या टैब नहीं मिला। मैनेजर से संपर्क करें।"
        message.startsWith("Payment saved.") -> "भुगतान सुरक्षित है। Shop में सिंक जारी रहेगा।"
        message.startsWith("Order ") && message.contains(" sent to the manager.") -> "ऑर्डर मैनेजर को भेज दिया गया है।"
        message.endsWith(" saved to customers.") -> "ग्राहक की जानकारी सहेज दी गई।"
        message.startsWith("Scanned ") -> "स्कैन हुआ: ${message.removePrefix("Scanned ")}"
        message.startsWith("Cloth saved, but") -> "कपड़ा सहेजा गया, लेकिन सूची अपडेट नहीं हुई। कपड़ों को सिंक करके QR लेबल बनाएँ।"
        else -> "कुछ समस्या हुई। कृपया फिर कोशिश करें।"
    }
}

val clothCategories = listOf(
    "Innerwear · बनियान / अंडरवियर",
    "Socks & Handkerchiefs · मोज़े / रुमाल",
    "Nightwear & Home Wear · नाइटी / घर के कपड़े",
    "Dhoti, Lungi & Gamchha · धोती / लुंगी / गमछा",
    "Blankets & Razais · कंबल / रज़ाई",
    "Shawls, Loi & Stoles · शॉल / लोई / स्टोल",
    "Thermals · थर्मल",
    "Sweaters & Jackets · स्वेटर / जैकेट",
    "Winter Accessories · टोपी / मफलर / दस्ताने",
    "Sarees · साड़ियाँ",
    "Shirting · शर्टिंग",
    "Suiting · सूटिंग",
    "Blouses & Petticoats · ब्लाउज़ / पेटीकोट",
    "Suit Materials & Dupattas · सूट का कपड़ा / दुपट्टा",
    "Women’s Readymade · लेडीज़ रेडीमेड",
    "Wedding & Party Wear · शादी / पार्टी के कपड़े",
    "Men’s Readymade · जेंट्स रेडीमेड",
    "Men’s Ethnic Wear · kurta / पायजामा",
    "Kids’ Clothing · बच्चों के कपड़े",
    "Baby Wear & Cloth Essentials · नवजात बच्चों का सामान",
    "School Uniforms · स्कूल ड्रेस",
    "Bedsheets & Bed Covers · चादर / बेडशीट",
    "Pillows & Toshaks · तकिया / तोशक",
    "Mosquito Nets · मच्छरदानी",
    "Towels · तौलिया",
    "Curtains & Furnishing Covers · पर्दा / कवर",
    "Mats & Floor Textiles · दरी / पायदान",
    "Loose Fabric · मीटर वाला कपड़ा",
    "Tailoring Supplies · सिलाई का सामान",
    "Puja & Ceremony Cloth · पूजा का कपड़ा",
    "Stitching & Alterations · सिलाई / फिटिंग"
).sortedBy { it.substringBefore(" ·").trim().lowercase() }
