#!/usr/bin/env node
// Applies audited JSX translation fixes. No Git operations and no writes outside 9 target files.
const fs=require('fs'),path=require('path');
const ts=require(path.join(process.cwd(),'apps/web/node_modules/typescript'));
const root=process.cwd();
if(!fs.existsSync(path.join(root,'apps/web/lib/i18n.ts')))throw Error('Run from HMS repository root (/workspace).');
const dictionary={"Choose every job this person performs. OWNER access cannot be assigned to employees.": ["Sélectionnez toutes les fonctions exercées par cette personne. Le rôle PROPRIÉTAIRE ne peut pas être attribué aux employés.", "Hitamo imirimo yose uyu muntu akora. Uburenganzira bwa NYIR'IKIGO ntibuhabwa abakozi."], "Reference:": ["Référence :", "Icyerekana :"], "Served by:": ["Servi par :", "Wakiriye :"], "Supply date:": ["Date de livraison :", "Itariki yo gutanga ibicuruzwa :"], "Payment due:": ["Échéance du paiement :", "Itariki ntarengwa yo kwishyura :"], "Currency:": ["Devise :", "Ifaranga :"], "Received:": ["Reçu :", "Ibyakiriwe :"], "Exchange rate:": ["Taux de change :", "Igipimo cy'ivunjisha :"], "Track movements": ["Suivre les mouvements", "Kurikirana imigendekere y'ububiko"], "Delete": ["Supprimer", "Siba"], "This makes the product inactive and removes it from new sales and purchases. Existing stock, invoices and history are kept. You can reactivate it using Edit.": ["Le produit sera désactivé et ne pourra plus être utilisé dans de nouvelles ventes ou achats. Le stock, les factures et l'historique restent conservés. Vous pouvez le réactiver avec Modifier.", "Iki gicuruzwa kizahagarikwa ntikongere gukoreshwa mu kugura cyangwa kugurisha bishya. Ububiko, inyemezabuguzi n'amateka biragumaho. Ushobora kongera kugikoresha ukoresheje Hindura."], "Confirm delete": ["Confirmer la suppression", "Emeza gusiba"], "Enter your normal purchase and selling prices once. KBP converts quantities and prices automatically.": ["Saisissez une seule fois vos prix habituels d'achat et de vente. KBP convertit automatiquement les quantités et les prix.", "Andika rimwe ibiciro bisanzwe byo kugura no kugurisha. KBP ihita ihindura ingano n'ibiciro."], "1. Product information": ["1. Informations sur le produit", "1. Amakuru y'igicuruzwa"], "Choose how this product is counted in stock. You can purchase it in larger packages.": ["Choisissez l'unité de suivi du produit en stock. Vous pouvez l'acheter en emballages plus grands.", "Hitamo igipimo cyo kubariramo iki gicuruzwa mu bubiko. Ushobora kukigura mu mapaki manini."], "Product name": ["Nom du produit", "Izina ry'igicuruzwa"], "Smallest tracked unit": ["Plus petite unité suivie", "Igipimo gito kibarirwamo"], "Stock is stored in this unit; it cannot change after creation.": ["Le stock est enregistré dans cette unité ; elle ne peut pas être modifiée après la création.", "Ububiko bubarirwa muri iki gipimo; ntigihindurwa nyuma yo kurema igicuruzwa."], "Custom stock unit": ["Unité de stock personnalisée", "Igipimo cy'ububiko cyihariye"], "This existing product tracks stock in": ["Ce produit existant suit le stock en", "Iki gicuruzwa gisanzwe kibarirwa mu"], "s. Do not enter the number of bottles/kg inside a package as the stock quantity. Preserve historical stock; create a new base-unit product after reconciliation if you need to sell individual contents.": [". Ne saisissez pas le nombre de bouteilles ou de kilogrammes par emballage comme quantité en stock. Conservez l'historique ; créez un nouveau produit après rapprochement pour vendre les unités individuelles.", ". Ntukandike amacupa cyangwa ibiro biri mu ipaki nk'umubare w'ububiko. Gumana amateka; niba ukeneye kugurisha ibiri imbere buri kimwe, banza uhuze imibare maze ureme igicuruzwa gishya."], "2. How you purchase it": ["2. Comment vous l'achetez", "2. Uko ugura iki gicuruzwa"], "Enter the amount paid for the actual purchase unit; no division required.": ["Saisissez le prix payé pour l'unité réellement achetée, sans division.", "Andika amafaranga wishyuye ku gipimo waguze; nta kugabanya gukenewe."], "Choose an existing package to edit": ["Choisir un emballage existant à modifier", "Hitamo ipaki isanzwe yo guhindura"], "+ Use base unit / create new package": ["+ Utiliser l'unité de base / créer un emballage", "+ Koresha igipimo cy'ibanze / rema ipaki nshya"], "Purchase unit": ["Unité d'achat", "Igipimo cyo kugura"], "Other / custom package": ["Autre / emballage personnalisé", "Indi paki / ipaki yihariye"], "Custom package name": ["Nom de l'emballage personnalisé", "Izina ry'ipaki yihariye"], "How many": ["Combien de", "Ni bingahe"], "in one": ["dans un", "muri imwe"], "Registered purchase cost per": ["Coût d'achat enregistré par", "Igiciro cy'ubuguzi cyanditswe kuri"], "Automatic cost per": ["Coût automatique par", "Igiciro kibarwa ku"], "(from the": ["(à partir du", " (kivuye ku"], "purchase price). You do not need to calculate or enter this.": ["prix d'achat). Aucun calcul ni saisie supplémentaire n'est nécessaire.", "giciro cyo kugura). Ntukeneye kubibara cyangwa kubyandika."], "Additional unit name": ["Nom de l'unité supplémentaire", "Izina ry'ikindi gipimo"], "Remove this package": ["Supprimer cet emballage", "Kuraho iyi paki"], "+ Add another buying or selling unit": ["+ Ajouter une autre unité d'achat ou de vente", "+ Ongeraho ikindi gipimo cyo kugura cyangwa kugurisha"], "3. How you sell it": ["3. Comment vous le vendez", "3. Uko ugurisha iki gicuruzwa"], "Retail can be per individual stock unit, while wholesale can be per package.": ["La vente au détail peut se faire à l'unité de stock et la vente en gros par emballage.", "Kugurisha ku giti bishobora gukorwa ku gipimo kimwe, naho kurangura bikaba ku ipaki."], "Retail price for 1": ["Prix de détail pour 1", "Igiciro cyo kugurisha kimwe"], "Wholesale price for 1": ["Prix de gros pour 1", "Igiciro cyo kurangura kimwe"], "Retail price per": ["Prix de détail par", "Igiciro cyo kugurisha kuri"], "(optional)": ["(facultatif)", "(si ngombwa)"], "Minimum selling price per": ["Prix de vente minimum par", "Igiciro gito cyo kugurisha kuri"], "Low-stock alert (in": ["Alerte de stock faible (en", "Imenyesha ry'ububiko buke (muri"], "Discount and return rules (optional)": ["Règles de remise et de retour (facultatif)", "Amategeko yo kugabanya ibiciro no gusubiza (si ngombwa)"], "Allow authorized discounts": ["Autoriser les remises approuvées", "Emerera kugabanya ibiciro byemewe"], "Maximum cashier discount (%)": ["Remise maximale du caissier (%)", "Igabanyirizwa ntarengwa ry'umukarani (%)"], "Customer return period (days)": ["Délai de retour client (jours)", "Iminsi umukiriya yemerewe gusubizamo"], "Allow fractional stock units (e.g., 0.5 kg; not 0.5 phone)": ["Autoriser les fractions d'unités (ex. : 0,5 kg, mais pas 0,5 téléphone)", "Emerera ibipimo bituzuye (urugero 0,5 kg; si 0,5 telefoni)"], "Manager discounts still require a reason; amounts below the minimum selling price are rejected. An item such as a phone should not allow fractional units.": ["Les remises du responsable exigent toujours une justification ; les prix inférieurs au minimum sont refusés. Un article comme un téléphone ne doit pas permettre de fractions.", "Kugabanya igiciro byemejwe n'umuyobozi bigomba kugira impamvu; igiciro kiri munsi y'igito cyemewe nticyakirwa. Igicuruzwa nka telefoni ntikigomba kugabanywamo uduce."], "Branch-specific price (optional)": ["Prix propre à la succursale (facultatif)", "Igiciro cy'ishami (si ngombwa)"], "Selling price per": ["Prix de vente par", "Igiciro cyo kugurisha kuri"], "Effective from": ["Applicable à partir du", "Bitangira gukurikizwa ku"], "One Save updates the product and its units together. Stock is added only through goods receiving.": ["Une seule sauvegarde met à jour le produit et ses unités. Le stock augmente uniquement lors de la réception des marchandises.", "Kubika rimwe bivugurura igicuruzwa n'ibipimo byacyo. Ububiko bwiyongera gusa igihe ibicuruzwa byakiriwe."], "Receive": ["Réceptionner", "Akira"], "Ordered:": ["Commandé :", "Ibyatumijwe :"], "· Accepted:": ["· Accepté :", "· Ibyemewe :"], "· Claimed:": ["· Réclamé :", "· Ibyasabiwe indishyi :"], "· Still to check:": ["· Reste à vérifier :", "· Ibisigaye kugenzurwa :"], "All quantities have been received or recorded as supplier claims.": ["Toutes les quantités ont été reçues ou déclarées en réclamation auprès du fournisseur.", "Ingano zose zarakiriwe cyangwa zanditswe nk'ibibazo bigomba gukemurwa n'uwatanze ibicuruzwa."], "Confirm this delivery": ["Confirmer cette livraison", "Emeza iri tungwa ry'ibicuruzwa"], "Add": ["Ajouter", "Ongeraho"], "to sellable stock.": ["au stock disponible à la vente.", "mu bubiko bushobora kugurishwa."], "Damaged:": ["Endommagé :", "Ibyangiritse :"], "· Missing:": ["· Manquant :", "· Ibyabuze :"], "will remain to check. Confirm only after checking the actual goods.": ["resteront à vérifier. Confirmez uniquement après vérification physique.", "bizasigara bigomba kugenzurwa. Emeza umaze kugenzura ibicuruzwa nyabyo."], "Confirm received goods": ["Confirmer les marchandises reçues", "Emeza ibicuruzwa byakiriwe"], "Back to quantities": ["Retour aux quantités", "Subira ku ngano"], "Accepted quantity (": ["Quantité acceptée (", "Ingano yemewe ("], "All remaining goods arrived in good condition": ["Toutes les marchandises restantes sont arrivées en bon état", "Ibicuruzwa byose byari bisigaye byageze bimeze neza"], "Damaged or missing goods": ["Marchandises endommagées ou manquantes", "Ibicuruzwa byangiritse cyangwa byabuze"], "Damaged quantity": ["Quantité endommagée", "Ingano yangiritse"], "Missing quantity": ["Quantité manquante", "Ingano yabuze"], "Next action": ["Action suivante", "Igikorwa gikurikira"], "Hold / inspect": ["Mettre en attente / inspecter", "Bika utegereje / genzura"], "Request replacement": ["Demander un remplacement", "Saba ibindi byo gusimbuza"], "Request supplier credit": ["Demander un avoir fournisseur", "Saba ideni ryishyurwa n'uwatanze ibicuruzwa"], "Delivery note / reason": ["Bon de livraison / motif", "Inyandiko y'ibyazanywe / impamvu"], "Damaged and missing goods are recorded as claims. They do not increase stock or automatically issue a refund.": ["Les articles endommagés et manquants sont enregistrés comme réclamations. Ils n'augmentent pas le stock et ne déclenchent pas de remboursement automatique.", "Ibyangiritse n'ibyabuze byandikwa nk'ibirego ku wabitanze. Ntibyongera ububiko kandi ntibisubiza amafaranga byikora."], "The total exceeds the quantity still to check.": ["Le total dépasse la quantité restant à vérifier.", "Igiteranyo kirenze ingano isigaye kugenzurwa."], "Enter a reason for damaged or missing goods.": ["Indiquez le motif des marchandises endommagées ou manquantes.", "Andika impamvu y'ibyangiritse cyangwa ibyabuze."], "Review received goods": ["Vérifier les marchandises reçues", "Suzuma ibicuruzwa byakiriwe"], "Review the details before confirming.": ["Vérifiez les détails avant de confirmer.", "Banza usuzume amakuru mbere yo kwemeza."], "Adjust or transfer stock": ["Ajuster ou transférer le stock", "Kosora cyangwa wimure ububiko"], "· Stock movements": ["· Mouvements de stock", "· Imigendekere y'ububiko"], "Latest 500 movements for this product at the selected stock location. Positive quantities add stock; negative quantities remove it.": ["Les 500 derniers mouvements de ce produit à l'emplacement sélectionné. Les quantités positives augmentent le stock ; les négatives le diminuent.", "Imigendekere 500 iheruka y'iki gicuruzwa mu bubiko bwatoranyijwe. Ingano nziza yongera ububiko; ingano mbi irabugabanya."], "Commerce setup required": ["Configuration commerciale requise", "Igenamiterere ry'ubucuruzi rirakenewe"], "Purchasing and selling need Inventory, Retail and Accounting modules. These are core Commerce operations.": ["Les achats et les ventes nécessitent les modules Stocks, Vente au détail et Comptabilité. Ce sont des fonctions essentielles du Commerce.", "Kugura no kugurisha bisaba ibice by'Ububiko, Kugurisha ku giti n'Ibaruramari. Ni ibikorwa by'ingenzi by'Ubucuruzi."], "Enable purchasing and sales": ["Activer les achats et les ventes", "Fungura kugura no kugurisha"], "Ask the business owner to enable Commerce operations.": ["Demandez au propriétaire d'activer les opérations commerciales.", "Saba nyir'ikigo gufungura ibikorwa by'ubucuruzi."], "Before continuing:": ["Avant de continuer :", "Mbere yo gukomeza :"], "choose the branch and the stock location in the selectors at the top of this page. You can then review and post your": ["choisissez la succursale et l'emplacement du stock en haut de cette page. Vous pourrez ensuite vérifier et enregistrer vos", "hitamo ishami n'ububiko mu mahitamo ari hejuru y'uru rupapuro. Hanyuma ushobora gusuzuma no kwandika"], "Choose products and quantities": ["Choisir les produits et les quantités", "Hitamo ibicuruzwa n'ingano"], "Review total and payment": ["Vérifier le total et le paiement", "Suzuma igiteranyo n'ubwishyu"], "lines)": ["lignes)", "imirongo)"], "No active products. Create a product in Products first.": ["Aucun produit actif. Créez d'abord un produit dans Produits.", "Nta bicuruzwa bikora. Banza ureme igicuruzwa ahitwa Ibicuruzwa."], "Qty": ["Qté", "Ingano"], "Catalog price (": ["Prix du catalogue (", "Igiciro cyo ku rutonde ("], "Line estimate": ["Estimation de la ligne", "Ikigereranyo cy'uyu murongo"], "Actions": ["Actions", "Ibikorwa"], "Available:": ["Disponible :", "Ibihari :"], "Override:": ["Remplacement :", "Igiciro gihinduwe :"], "Before discounts / tax": ["Avant remises / taxes", "Mbere yo kugabanya ibiciro / umusoro"], "Add a product above to begin.": ["Ajoutez un produit ci-dessus pour commencer.", "Ongeraho igicuruzwa hejuru kugira ngo utangire."], "Fixed amount (": ["Montant fixe (", "Amafaranga adahinduka ("], "Percentage (%)": ["Pourcentage (%)", "Ijanisha (%)"], "Only one item expands at a time. The product list scrolls independently, including purchases with 20 or more lines.": ["Un seul article peut être développé à la fois. La liste des produits défile indépendamment, même pour les achats de 20 lignes ou plus.", "Igicuruzwa kimwe gusa kiramburwa icyarimwe. Urutonde rw'ibicuruzwa rushobora kuzamurwa no kumanurwa ukwabwo, no ku byaguzwe bifite imirongo 20 cyangwa irenga."], "Product lines": ["Lignes de produits", "Imirongo y'ibicuruzwa"], "Quantity total (mixed units)": ["Quantité totale (unités mixtes)", "Igiteranyo cy'ingano (ibipimo bitandukanye)"], "By item": ["Par article", "Kuri buri gicuruzwa"], "Final discounts, taxes and exchange rates are calculated by the backend when you review.": ["Les remises finales, taxes et taux de change sont calculés par le serveur lors de la vérification.", "Igabanyirizwa ry'ibiciro rya nyuma, imisoro n'ibipimo by'ivunjisha bibarwa na seriveri igihe usuzuma."], "Reviewed total": ["Total vérifié", "Igiteranyo cyasuzumwe"], "Confirmation is uncertain. Retry the same request to avoid duplicates.": ["Confirmation incertaine. Réessayez la même demande pour éviter les doublons.", "Kwemeza ntibizwi neza. Ongera wohereze icyifuzo kimwe kugira ngo wirinde ibyanditswe kabiri."], "Retry same transaction": ["Réessayer la même transaction", "Ongera ugerageze igikorwa kimwe"], "Payment and checkout": ["Paiement et encaissement", "Kwishyura no kurangiza igurisha"], "Cash": ["Espèces", "Amafaranga y'intoki"], "Mobile Money": ["Paiement mobile", "Amafaranga kuri telefoni"], "Bank transfer": ["Virement bancaire", "Kohereza amafaranga kuri banki"], "Card": ["Carte bancaire", "Ikarita ya banki"], "Approved customer credit": ["Crédit client approuvé", "Ideni ry'umukiriya ryemewe"], "Amount to collect": ["Montant à encaisser", "Amafaranga yo kwakira"], "Automatically selected from reviewed cart; taxes and discounts included.": ["Sélectionné automatiquement d'après le panier vérifié, taxes et remises incluses.", "Bihita bitoranywa hashingiwe ku byasuzumwe, harimo imisoro n'igabanyirizwa ry'ibiciro."], "You have no open cashier shift. Go to": ["Aucune caisse ouverte. Allez à", "Nta shifuti y'umubitsi ifunguye. Jya kuri"], "to open one before accepting cash.": ["pour en ouvrir une avant d'accepter des espèces.", "kugira ngo uyifungure mbere yo kwakira amafaranga y'intoki."], "No active matching": ["Aucun compte actif correspondant", "Nta konti ikora ihuye na"], "account for": ["pour", "kuri"], ". The owner can create one in Payment accounts.": [". Le propriétaire peut en créer un dans Comptes de paiement.", ". Nyir'ikigo ashobora kuyirema muri Konti zo kwishyuriraho."], "The customer must have an approved credit limit. A credit sale leaves an amount receivable.": ["Le client doit disposer d'une limite de crédit approuvée. Une vente à crédit crée une créance.", "Umukiriya agomba kuba afite umupaka w'ideni wemewe. Kugurisha ku ideni bituma habaho amafaranga yishyuzwa."], "Invoice cost and supplier discount are calculated above. Receive goods separately after posting. Payment can be recorded later or added below.": ["Le montant facturé et la remise fournisseur sont calculés ci-dessus. Réceptionnez les marchandises séparément après l'enregistrement. Le paiement peut être ajouté ci-dessous ou ultérieurement.", "Igiciro cy'inyemezabuguzi n'igabanyirizwa ry'uwatanze ibicuruzwa bibarwa hejuru. Akira ibicuruzwa ukwabyo nyuma yo kwandika. Ubwishyu bushobora kwandikwa nyuma cyangwa munsi aha."], "Split payments / advanced payment entry": ["Paiements fractionnés / saisie avancée", "Kwishyura mu byiciro / kwandika ubwishyu mu buryo burambuye"], "Remove": ["Retirer", "Kuraho"], "Customers and suppliers": ["Clients et fournisseurs", "Abakiriya n'abatanga ibicuruzwa"], "· Edit": ["· Modifier", "· Hindura"], "Payment accounts": ["Comptes de paiement", "Konti zo kwishyuriraho"], "No payment accounts yet. Click Create payment account to add one.": ["Aucun compte de paiement. Cliquez sur Créer un compte de paiement pour en ajouter un.", "Nta konti zo kwishyuriraho zirahari. Kanda Rema konti yo kwishyuriraho kugira ngo wongere imwe."], "A4 / Save PDF": ["A4 / Enregistrer en PDF", "A4 / Bika nka PDF"], "Payments and refunds": ["Paiements et remboursements", "Ubwishyu n'amafaranga asubizwa"], "Record a return": ["Enregistrer un retour", "Andika ibyagaruwe"], "Supplier claim:": ["Réclamation fournisseur :", "Ikirego ku watanze ibicuruzwa :"], "· Damaged": ["· Endommagé", "· Ibyangiritse"], "· Missing": ["· Manquant", "· Ibyabuze"], "The owner must enable Accounting and approve opening inventory valuation before shifts can open.": ["Le propriétaire doit activer la comptabilité et approuver la valorisation initiale des stocks avant l'ouverture des caisses.", "Nyir'ikigo agomba gufungura Ibaruramari no kwemeza agaciro k'ububiko bw'ibanze mbere yo gufungura shifuti."], "Select a branch before opening a shift.": ["Sélectionnez une succursale avant d'ouvrir une caisse.", "Hitamo ishami mbere yo gufungura shifuti."], "No active cash account exists for this business. Opening a cashier shift requires a cash payment account.": ["Aucun compte espèces actif pour cette entreprise. L'ouverture d'une caisse exige un compte de paiement en espèces.", "Nta konti y'amafaranga y'intoki ikora muri iki kigo. Gufungura shifuti y'umubitsi bisaba konti y'amafaranga y'intoki."], "Create cash account": ["Créer un compte espèces", "Rema konti y'amafaranga y'intoki"], "Ask the owner to create a cash account under Payment accounts.": ["Demandez au propriétaire de créer un compte espèces sous Comptes de paiement.", "Saba nyir'ikigo kurema konti y'amafaranga y'intoki muri Konti zo kwishyuriraho."], "You already have an open shift on": ["Vous avez déjà une caisse ouverte le", "Usanzwe ufite shifuti ifunguye ku"], ". Close it before opening another.": [". Fermez-la avant d'en ouvrir une autre.", ". Yifunge mbere yo gufungura indi."], "Payment account balances": ["Soldes des comptes de paiement", "Amafaranga asigaye kuri konti zo kwishyuriraho"], "Cumulative recorded balance through": ["Solde cumulé enregistré jusqu'au", "Amafaranga yose yanditswe kugeza ku"], "for the selected branch scope. Includes receipts, supplier payments, refunds and expenses. Unrecorded opening funds and cash drawer floats are excluded. Each account is shown in its own currency.": ["pour la portée des succursales sélectionnées. Comprend les recettes, paiements fournisseurs, remboursements et dépenses. Les fonds d'ouverture et fonds de caisse non enregistrés sont exclus. Chaque compte est affiché dans sa devise.", "ku mashami yatoranyijwe. Harimo amafaranga yakiriwe, ayishyuwe abatanga ibicuruzwa, ayasubijwe n'ibyakoreshejwe. Amafaranga yo gutangira atanditswe ntabarwamo. Buri konti igaragazwa mu ifaranga ryayo."], "Inactive account": ["Compte inactif", "Konti idakora"], "No payment accounts found.": ["Aucun compte de paiement trouvé.", "Nta konti yo kwishyuriraho yabonetse."], "Final discounts, taxes and exchange rates are calculated bythe backend when you review.": ["Les remises finales, taxes et taux de change sont calculés par le serveur lors de la vérification.", "Igabanyirizwa ry’ibiciro rya nyuma, imisoro n’ivunjisha bibarwa na seriveri igihe usuzuma."], "Latest 500 movements for this product at the selected stocklocation. Positive quantities add stock; negative quantities remove it.": ["Les 500 derniers mouvements du produit à cet emplacement. Les quantités positives augmentent le stock, les négatives le réduisent.", "Imigendekere 500 iheruka y’igicuruzwa muri ubu bubiko. Ingano nziza yongera ububiko, ingano mbi irabugabanya."], "Manager discounts still require a reason; amounts below theminimum selling price are rejected. An item such as a phone should not allow fractional units.": ["Les remises du responsable exigent une justification ; les prix inférieurs au minimum sont refusés. Un téléphone ne se vend pas en fractions.", "Kugabanya ibiciro by’umuyobozi bisaba impamvu; igiciro kiri munsi y’igito nticyemerwa. Telefoni ntigomba kugabanywamo uduce."]};

// Additional common labels confirmed by the complete JSX audit.
Object.assign(dictionary, {
  "Product": [
    "Produit",
    "Igicuruzwa"
  ],
  "Unit price": [
    "Prix unitaire",
    "Igiciro cya kimwe"
  ],
  "Tax": [
    "Taxe",
    "Umusoro"
  ],
  "Total": [
    "Total",
    "Igiteranyo"
  ],
  "Subtotal": [
    "Sous-total",
    "Igiteranyo mbere y’umusoro"
  ],
  "Outstanding": [
    "Solde restant",
    "Amafaranga asigaye kwishyurwa"
  ],
  "Payments": [
    "Paiements",
    "Ubwishyu"
  ],
  "Cancel": [
    "Annuler",
    "Hagarika"
  ],
  "SKU": [
    "Référence produit",
    "Kode y’igicuruzwa"
  ],
  "Barcode (optional)": [
    "Code-barres (facultatif)",
    "Barkode (si ngombwa)"
  ],
  "Category": [
    "Catégorie",
    "Icyiciro"
  ],
  "Description": [
    "Description",
    "Ibisobanuro"
  ],
  "Tax rate (%)": [
    "Taux de taxe (%)",
    "Igipimo cy’umusoro (%)"
  ],
  "Close": [
    "Fermer",
    "Funga"
  ],
  "Edit": [
    "Modifier",
    "Hindura"
  ],
  "Reference": [
    "Référence",
    "Ikiranga inyandiko"
  ],
  "Currency": [
    "Devise",
    "Ifaranga"
  ],
  "Supply date": [
    "Date de livraison",
    "Itariki yo gutanga ibicuruzwa"
  ],
  "Cashier shifts": [
    "Sessions de caisse",
    "Shifuti z’ababitsi"
  ],
  "Add payment": [
    "Ajouter un paiement",
    "Ongeraho ubwishyu"
  ]
});
const targets=['CommerceConfiguration','CommerceInvoice','CommercePanel','CommerceProductEditor','CommerceReceivingForm','CommerceReviewDialog','CommerceStockOperations','CommerceTrading'].map(f=>'apps/web/components/commerce/'+f+'.tsx');
const i18n='apps/web/lib/i18n.ts';
const data=fs.readFileSync(path.join(root,i18n),'utf8');
const match=/const rows=`([\s\S]*?)`;/m.exec(data);
if(!match)throw Error('Cannot locate translation rows.');
const existing=new Set(match[1].split('\n').filter(x=>x.trim()).map(x=>x.split('|')[0]));
const staged=new Map();let changedNodes=0;const keys=new Set();
for(const f of targets){
 const p=path.join(root,f),src=fs.readFileSync(p,'utf8');
 const ast=ts.createSourceFile(p,src,ts.ScriptTarget.Latest,true,ts.ScriptKind.TSX),replacements=[];
 let lacksHook=false;let bodyStart=-1;
 function visit(node){
  if(ts.isFunctionDeclaration(node)&&node.modifiers?.some(m=>m.kind===ts.SyntaxKind.DefaultKeyword)&&node.body)bodyStart=node.body.getStart(ast)+1;
  if(ts.isJsxText(node)){
   const key=node.text.trim();
   if(/[A-Za-z]{3}/.test(key)&&!key.startsWith('HotelPro')&&!['English','Français','Kinyarwanda'].includes(key)){
    if(!existing.has(key)&&!dictionary[key])throw Error('No translation for '+f+': '+JSON.stringify(key));
    const raw=src.slice(node.pos,node.end),start=raw.match(/^\s*/)[0],end=raw.match(/\s*$/)[0];
    const translator=f.endsWith('/CommercePanel.tsx')?'translate':'t';
    replacements.push([node.pos,node.end,start+'{'+translator+'('+JSON.stringify(key)+')}'+end]);
    keys.add(key);changedNodes++;
   }
  }
  ts.forEachChild(node,visit);
 }
 visit(ast);
 if(!replacements.length)continue;
 if(f.endsWith('/CommercePanel.tsx')&&!/const\s*\{\s*t\s*:\s*translate\s*\}\s*=\s*useLocale\(/.test(src))throw Error('CommercePanel translation alias changed: abort.');
 if(!f.endsWith('/CommercePanel.tsx')&&!/\bconst\s*\{\s*t\s*\}\s*=\s*useLocale\(/.test(src)){
  if(bodyStart===-1)throw Error('Cannot locate component body: '+f);
  if(!src.includes('useLocale')){
   const firstLineEnd=src.indexOf('\n');
   replacements.push([firstLineEnd,firstLineEnd,'\nimport {useLocale} from "@/components/LocaleProvider";']);
  }
  replacements.push([bodyStart,bodyStart,'\n const {t}=useLocale();']);
 }
 for(let i=0;i<replacements.length;i++)for(let j=i+1;j<replacements.length;j++){
  if(replacements[i][0]<replacements[j][1]&&replacements[j][0]<replacements[i][1])throw Error('Overlapping JSX replacements: '+f);
 }
 let out=src;
 for(const [a,b,v] of replacements.sort((x,y)=>y[0]-x[0]))out=out.slice(0,a)+v+out.slice(b);
 const parse=ts.createSourceFile(f,out,ts.ScriptTarget.Latest,true,ts.ScriptKind.TSX);
 if(parse.parseDiagnostics.length)throw Error('TSX parse error after edit '+f+': '+parse.parseDiagnostics.map(x=>x.messageText).join('; '));
 staged.set(f,out);
}
const missing=[...keys].filter(k=>!existing.has(k));
const sep=data.includes('\r\n')?'\r\n':'\n';
const translated=missing.map(k=>{
 const pair=dictionary[k];if(!pair||pair.some(v=>!v||v.includes('|')||v.includes('\n')))throw Error('Bad translations: '+k);
 return [k,...pair].join('|');
});
if(translated.length){
 const offset=match.index+'const rows=`'.length+match[1].length;
 let rows=match[1];
 const toAdd=translated.join('\n');
 rows=rows.replace(/\s*$/,'')+'\n'+toAdd+'\n';
 staged.set(i18n,data.slice(0,match.index)+'const rows=`'+rows+'`;'+data.slice(match.index+match[0].length));
}
console.log('Target files:',staged.size,'JSX fragments:',changedNodes,'new translations:',translated.length);
const dry=process.argv.includes('--dry-run');
if(!dry){
 for(const [f,s] of staged){
  const dest=path.join(root,f),before=fs.readFileSync(dest,'utf8');
  // Keep local rollback snapshot adjacent to each changed file, outside Git staging.
  const backup=dest+'.kbp-translations-backup';
  if(fs.existsSync(backup))throw Error('Backup already exists; aborting to preserve it: '+backup);
 }
 for(const [f,s] of staged){
  const dest=path.join(root,f),before=fs.readFileSync(dest,'utf8'),backup=dest+'.kbp-translations-backup';
  fs.writeFileSync(backup,before);fs.writeFileSync(dest,before.includes('\r\n')?s.replace(/\r?\n/g,'\r\n'):s);
  console.log('Updated',f);
 }
}
else console.log('DRY RUN: no files written');
