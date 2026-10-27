export const locales=["en","fr","rw"] as const;
export type Locale=(typeof locales)[number];
const rows=`
Parent category (optional)|Catégorie parente (facultative)|Icyiciro gikuru (si ngombwa)
Business settings|Paramètres de l’entreprise|Igenamiterere ry’ikigo
Decimal places|Décimales|Imibare nyuma y’akitso
Effective date|Date d’effet|Itariki bitangiriraho
Source|Source|Inkomoko
Allowed branches|Succursales autorisées|Amashami yemerewe
Configuration saved.|Configuration enregistrée.|Igenamiterere ryabitswe.
The base accounting currency is fixed after provisioning.|La devise comptable de base est fixe après la création.|Ifaranga ry’ibanze ntirihindurwa nyuma yo kwandikisha ikigo.
Enter the base-currency value of one transaction-currency unit. Existing rate records cannot be changed.|Saisissez la valeur en devise de base d’une unité de la devise de transaction. Les taux enregistrés sont immuables.|Andika agaciro k’ifaranga rimwe mu ifaranga ry’ibanze. Ibipimo byabitswe ntibihindurwa.
The change conflicts with existing records. Review the current configuration.|La modification est incompatible avec des données existantes. Vérifiez la configuration.|Impinduka zibangamiye amakuru asanzwe. Suzuma igenamiterere.
Stock movement ledger|Registre des mouvements de stock|Igitabo cy’imigendekere y’ibicuruzwa
Operation|Opération|Igikorwa
Stock adjustment|Ajustement de stock|Gukosora ibarura
Stock transfer|Transfert de stock|Kwimura ibicuruzwa
Destination location|Emplacement de destination|Ububiko byoherezwamo
Retry same request|Réessayer la même demande|Ongera ugerageze icyifuzo kimwe
Retry preserves the original request. Check the ledger before starting another operation.|La nouvelle tentative conserve la demande initiale. Vérifiez le registre avant une autre opération.|Kongera kugerageza bigumana icyifuzo cya mbere. Banza urebe igitabo mbere y’ikindi gikorwa.
Stock operation rejected. Check available quantity and the request details.|Opération refusée. Vérifiez la quantité disponible et la demande.|Igikorwa nticyemewe. Reba umubare uhari n’amakuru y’icyifuzo.
Unable to complete the stock operation. Retry the same request.|Impossible de terminer l’opération. Réessayez la même demande.|Igikorwa nticyarangiye. Ongera ugerageze icyifuzo kimwe.
Stock operation recorded.|Mouvement de stock enregistré.|Igikorwa cy’ububiko cyanditswe.
Audit history|Historique d’audit|Amateka y’igenzura
Currencies|Devises|Amafaranga
Contact|Coordonnées|Aho kubariza
VIEWER|Lecteur|Ureba amakuru gusa
Organization and owner created. Give the temporary password securely to the owner. They must change it at first login.|Organisation et propriétaire créés. Transmettez le mot de passe temporaire de manière sécurisée. Il doit être changé à la première connexion.|Ikigo na nyiracyo byanditswe. Muhe ijambo ry’ibanga ry’agateganyo mu buryo bwizewe. Agomba kurihindura akinjira bwa mbere.
Kivu Business Platform — Business Management Platform|Plateforme commerciale Kivu — Gestion des affaires|Urubuga rw’Ubucuruzi rwa Kivu — Imicungire y’ubucuruzi
Manage KBP organizations (Hotel / Commerce)|Gérer les organisations KBP (Hôtel / Commerce)|Cunga ibigo bya KBP (Hoteli / Ubucuruzi)

Kivu Business Platform · KBP Administration|Plateforme commerciale Kivu · Administration KBP|Urubuga rw’Ubucuruzi rwa Kivu · Ubuyobozi bwa KBP
Organizations and owners|Organisations et propriétaires|Ibigo na ba nyirabyo
Platform administration only. No access to operational customer records.|Administration uniquement. Aucune consultation des données opérationnelles des clients.|Ubuyobozi bw’urubuga gusa. Nta kureba amakuru y’ibikorwa by’abakiriya.
Hotel administration|Administration des hôtels|Ubuyobozi bw’amahoteri
Create organization and owner|Créer une organisation et son propriétaire|Andikisha ikigo na nyiracyo
Choose one product. Owners receive access only to their assigned organization.|Choisissez un produit. Chaque propriétaire accède uniquement à son organisation.|Hitamo porogaramu imwe. Nyir’ikigo abona amakuru y’ikigo cye gusa.
Product|Produit|Porogaramu
Organization name|Nom de l'organisation|Izina ry’ikigo
Hotel code|Code de l'hôtel|Kode ya hoteli
Owner's full name|Nom complet du propriétaire|Amazina ya nyir’ikigo
Owner email|E-mail du propriétaire|Imeyili ya nyir’ikigo
Temporary password (15–128 characters)|Mot de passe temporaire (15 à 128 caractères)|Ijambo ry’ibanga ry’agateganyo (inyuguti 15–128)
Base currency|Devise de base|Ifaranga rikoreshwa
Preferred language|Langue préférée|Ururimi rwatoranyijwe
Provide the temporary password privately. Never send passwords in an unencrypted group chat.|Transmettez le mot de passe temporaire en privé, jamais dans un groupe non sécurisé.|Tanga ijambo ry’ibanga ry’agateganyo mu ibanga, nturishyire mu itsinda ridakingiwe.
Creating…|Création…|Birimo gukorwa…
Create owner and organization|Créer le propriétaire et l'organisation|Andikisha nyir’ikigo n’ikigo
Registered organizations|Organisations enregistrées|Ibigo byanditswe
Name|Nom|Izina
Status|Statut|Imiterere
No organizations found.|Aucune organisation trouvée.|Nta bigo byabonetse.

Kivu Business Platform (KBP)|Plateforme commerciale Kivu (KBP)|Urubuga rw’Ubucuruzi rwa Kivu (KBP)
Built around your business|Conçu pour votre activité|Byagenewe ubucuruzi bwawe
One platform. Smarter business management.|Une plateforme. Une gestion plus efficace.|Urubuga rumwe. Imicungire inoze y'ubucuruzi.
Connected workspaces for hotels, retailers, wholesalers and distributors. Choose the tools that fit your business.|Des espaces connectés pour les hôtels, détaillants, grossistes et distributeurs. Choisissez les outils adaptés à votre activité.|Ahantu ho gukorera ku mahoteri, abacuruza ku giti, abarangura n'abakwirakwiza ibicuruzwa. Hitamo ibikoresho bijyanye n'ubucuruzi bwawe.
For hospitality|Pour l'hôtellerie|Ku mahoteri
Hotel Management System|Système de gestion hôtelière|Sisitemu yo gucunga hoteli
Keep your front desk, guest services and daily operations connected.|Reliez votre réception, vos services aux clients et vos opérations quotidiennes.|Huza aho bakirira abantu, serivisi z'abashyitsi n'imirimo ya buri munsi.
Reservations and rooms|Réservations et chambres|Ibika n'ibyumba
Guest management|Gestion des clients|Gucunga abashyitsi
Restaurant and bar|Restaurant et bar|Resitora n'akabari
Purchasing and stock|Achats et stocks|Kugura n'ububiko
Payments and reporting|Paiements et rapports|Ubwishyu na raporo
Access Hotel Management|Accéder à la gestion hôtelière|Fungura imicungire ya hoteli
For trade and distribution|Pour le commerce et la distribution|Ku bucuruzi no gukwirakwiza ibicuruzwa
Inventory, Sales & Distribution|Stocks, ventes et distribution|Ububiko, igurisha no gukwirakwiza ibicuruzwa
Build your business catalogue and organize stock across branches and warehouses.|Créez votre catalogue et organisez les stocks de vos succursales et entrepôts.|Tegura urutonde rw'ibicuruzwa n'ububiko mu mashami no mu bigega.
Businesses, branches and stock locations|Entreprises, succursales et emplacements de stock|Ibigo, amashami n'ahabikwa ibicuruzwa
Categories, products and units|Catégories, produits et unités|Ibyiciro, ibicuruzwa n'ibipimo
Opening stock and stock balances|Stock initial et soldes de stock|Ibicuruzwa by'ibanze n'ibisigaye mu bubiko
Coming next|À venir|Ibizakurikiraho
Retail and wholesale sales, invoicing, distribution and accounting.|Ventes au détail et en gros, facturation, distribution et comptabilité.|Kugurisha ku giti no ku bwinshi, inyemezabuguzi, gukwirakwiza ibicuruzwa n'ibaruramari.
Access Inventory & Sales|Accéder aux stocks et ventes|Fungura ububiko n'igurisha
One account. Access is managed separately for each workspace.|Un seul compte. Les accès sont gérés séparément pour chaque espace.|Konti imwe. Uburenganzira bucungwa ukwabwo kuri buri hantu ho gukorera.
Product navigation|Navigation des produits|Guhitamo serivisi
Hotel Management|Gestion hôtelière|Imicungire ya hoteli
Inventory & Sales|Stocks et ventes|Ububiko n'igurisha
Workspace navigation|Navigation des espaces|Guhitamo aho ukorera
Choose workspace|Choisir un espace|Hitamo aho ukorera
Workspace access|Accès à l'espace|Uburenganzira bwo gukorera
Back to platform|Retour à la plateforme|Subira ku rubuga
Sign in to your business platform|Connectez-vous à votre plateforme de gestion|Injira ku rubuga rw'ubucuruzi bwawe
Platform administration|Administration de la plateforme|Ubuyobozi bw'urubuga
Your session has expired. Please sign in again.|Votre session a expiré. Veuillez vous reconnecter.|Igihe cyo gukoresha konti cyarangiye. Ongera winjire.
Commerce is currently disabled. Contact your administrator for availability.|Le module commercial est désactivé. Contactez votre administrateur pour connaître sa disponibilité.|Serivisi y'ubucuruzi irafunze. Baza umuyobozi igihe izabonekera.
No commerce business is assigned to your account. Ask your administrator for access.|Aucune entreprise commerciale n'est affectée à votre compte. Demandez un accès à votre administrateur.|Nta kigo cy'ubucuruzi cyahawe konti yawe. Saba umuyobozi uburenganzira.
You do not have access to this workspace. Contact your administrator.|Vous n'avez pas accès à cet espace. Contactez votre administrateur.|Ntiwemerewe gukorera hano. Baza umuyobozi.
This workspace is temporarily unavailable. Please try again shortly.|Cet espace est temporairement indisponible. Veuillez réessayer bientôt.|Aha ho gukorera ntihaboneka ubu. Ongera ugerageze mu kanya.
No active hotel workspace is assigned to your account. Ask your administrator for access.|Aucun espace hôtelier actif n'est affecté à votre compte. Demandez un accès à votre administrateur.|Nta hoteli ifunguye yahawe konti yawe. Saba umuyobozi uburenganzira.
Commerce navigation|Navigation commerciale|Guhitamo serivisi z'ubucuruzi
WAREHOUSE|Entrepôt|Ikigega
SHOP_FLOOR|Espace de vente|Ahacururizwa
STOREROOM|Réserve|Ububiko
RETURNS|Retours|Ibyasubijwe
DAMAGED|Stock endommagé|Ibyangiritse

Commerce workspace|Espace commercial|Ubucuruzi
Business|Entreprise|Ikigo
Branch|Succursale|Ishami
Stock location|Emplacement du stock|Ububiko
Refresh|Actualiser|Ongera usubiremo
Overview|Aperçu|Incamake
Branches|Succursales|Amashami
Stock locations|Emplacements|Ububiko
Categories|Catégories|Ibyiciro
Products|Produits|Ibicuruzwa
Stock|Stock|Ibigega
No records yet|Aucune donnée|Nta makuru arahari
Commerce is unavailable or you don't have access. Ask your administrator to enable it.|Le module commercial est indisponible ou vous n'y avez pas accès.|Ubucuruzi ntiburafungurwa cyangwa ntubwemerewe.
Create|Créer|Ongeraho
Name|Nom|Izina
Currency|Devise|Ifaranga
SKU|Référence|Kode
Base unit|Unité de base|Igipimo
Purchase cost|Coût d'achat|Igiciro cyo kugura
Retail price|Prix de détail|Igiciro cyo kugurisha
Wholesale price|Prix de gros|Igiciro cya byinshi
Barcode (optional)|Code-barres (facultatif)|Barcode (si ngombwa)
Location type|Type d'emplacement|Ubwoko bw'ububiko
Save|Enregistrer|Bika
Cancel|Annuler|Hagarika
Loading...|Chargement...|Birimo gufunguka...
Create a branch first|Créez d'abord une succursale|Banza wongere ishami
Create a stock location first|Créez d'abord un emplacement|Banza wongere ububiko
Select|Sélectionner|Hitamo
Unable to load data|Chargement impossible|Ntibyashobotse
Sales, profit, invoices and inventory valuation will appear when their backend modules are implemented.|Les ventes, bénéfices, factures et valorisations des stocks apparaîtront lorsque les modules correspondants seront disponibles.|Ibyagurishijwe, inyungu, inyemezabuguzi n’agaciro k’ububiko bizagaragara igihe izo serivisi zizaba zakozwe.
Reservation details|Détails de la réservation|Ibisobanuro by'ibika
Open folio|Ouvrir le folio|Fungura inyandiko y'ubwishyu
Loading reservation details...|Chargement de la réservation...|Ibisobanuro by'ibika birimo gutegurwa...
View reservation|Voir la réservation|Reba ibika
Unable to load reservation details.|Impossible de charger les détails de la réservation.|Ibisobanuro by'ibika ntibyabonetse.
Account cash movements|Mouvements de trésorerie par compte|Amafaranga yinjiye n'asohotse kuri konti
Save name|Enregistrer le nom|Bika izina
Net movement in accounting currency|Mouvement net en devise comptable|Amafaranga yinjiye ukuyemo ayasohotse mu ifaranga ry'ibaruramari
Applied to purchase|Affecté à l'achat|Ayishyuwe ku igura
Exchange difference|Écart de change|Ikinyuranyo cy'ivunjisha
Original currency|Devise d'origine|Ifaranga ry'umwimerere
Reservation advance activity|Mouvements des avances de réservation|Ibikorwa by'amafaranga yatanzwe mbere ku ibika
Reservation advances are included in customer receipts when the money is received, but they are not billed revenue until accommodation is charged. Advance refunds are included in refunds.|Les avances de réservation sont incluses dans les encaissements à leur réception, mais deviennent des revenus facturés lorsque le séjour est facturé. Leurs remboursements sont inclus dans les remboursements.|Amafaranga yatanzwe mbere ku ibika abarwa mu yakiriwe igihe yakiriwe, ariko aba amafaranga yacurujwe iyo icumbi ryishyujwe. Ayasubijwe abarwa mu mafaranga yasubijwe.
Open Cashier|Ouvrir la caisse|Fungura aho bakirira ubwishyu
All statuses|Tous les statuts|Imimerere yose
Payment|Paiement|Ubwishyu
All payments|Tous les paiements|Ubwishyu bwose
No advance|Aucune avance|Nta mafaranga yatanzwe mbere
Partially prepaid|Partiellement prépayé|Igice cyishyuwe mbere
Fully prepaid|Entièrement prépayé|Byose byishyuwe mbere
Sort by|Trier par|Tondeka ukurikije
Operational priority|Priorité opérationnelle|Ibyihutirwa mu kazi
Arrival date - earliest first|Date d'arrivée - croissante|Itariki yo kuhagera - ibanza mbere
Arrival date - latest first|Date d'arrivée - décroissante|Itariki yo kuhagera - iheruka mbere
Checkout date - earliest first|Date de départ - croissante|Itariki yo kugenda - ibanza mbere
Checkout date - latest first|Date de départ - décroissante|Itariki yo kugenda - iheruka mbere
Customer A-Z|Client A-Z|Umukiriya A-Z
Customer Z-A|Client Z-A|Umukiriya Z-A
Reservation reference|Référence de réservation|Nimero y'ibika
reservations found|réservations trouvées|ibika ryabonetse
Clear filters|Effacer les filtres|Kuraho ibyatoranyijwe
Edit reservation|Modifier la réservation|Hindura ibika
Receive advance|Recevoir une avance|Akira amafaranga atanzwe mbere
Page|Page|Urupapuro
of|sur|muri
Previous|Précédent|Ibanza
Next|Suivant|Ikurikira
Assigned rooms|Chambres attribuées|Ibyumba byatanzwe
The system will verify that these rooms are still available for the new dates before saving.|Le système vérifiera la disponibilité des chambres aux nouvelles dates avant d'enregistrer.|Sisitemu iragenzura ko ibi byumba bikiboneka ku matariki mashya mbere yo kubika.
Check-in|Arrivée|Kwinjira
Check-out|Départ|Gusohoka
Current nights|Nuits actuelles|Amajoro asanzwe
New nights|Nouvelles nuits|Amajoro mashya
New total|Nouveau total|Igiteranyo gishya
Advance received|Avance reçue|Amafaranga yakiriwe mbere
If the new dates overlap another reservation, the change will be rejected. If shortening the stay makes the reservation total lower than an advance already received, the excess advance must be refunded first.|Si les nouvelles dates chevauchent une autre réservation, la modification sera refusée. Si le séjour raccourci coûte moins que l'avance reçue, remboursez d'abord l'excédent.|Niba amatariki mashya ahura n'irindi bika, impinduka zirangwa. Niba kugabanya igihe cyo gucumbika bituma ikiguzi kijya munsi y'amafaranga yakiriwe mbere, ayarenze abanza gusubizwa.
Receive reservation advance|Recevoir une avance de réservation|Akira amafaranga atanzwe mbere ku ibika
Reservation total|Total de la réservation|Igiteranyo cy'ibika
Remaining|Restant|Asigaye
The payment will be submitted for approval and will count as a receipt, not as revenue.|Le paiement sera soumis à approbation et compté comme encaissement, pas comme revenu.|Ubwishyu buroherezwa kwemezwa kandi bubarwe nk'amafaranga yakiriwe, aho kuba amafaranga yacurujwe.
Submit for approval|Soumettre à approbation|Ohereza kwemezwa
Overdue checkout|Départ en retard|Gusohoka byakererewe
Future arrival|Arrivée future|Kuhagera mu gihe kizaza
Check-in becomes available on|L'enregistrement sera disponible le|Kwinjira bizashoboka ku wa
Selling currency|Devise de vente|Ifaranga ryo kugurisha
Purchase currency|Devise d'achat|Ifaranga ryo kugura
Security|Sécurité|Umutekano
Forgot password?|Mot de passe oublié ?|Wibagiwe ijambo ry'ibanga?
Forgot password|Mot de passe oublié|Wibagiwe ijambo ry'ibanga
Reset password|Réinitialiser le mot de passe|Shyiraho ijambo ry'ibanga rishya
Change password|Changer le mot de passe|Hindura ijambo ry'ibanga
Send password recovery|Envoyer les instructions de récupération|Ohereza amabwiriza yo kugarura konti
Currencies and payment accounts|Devises et comptes de paiement|Amafaranga na konti zo kwishyuriraho
Enable currencies for prices and transactions. Set their rates below as the value of one unit in the hotel accounting currency.|Activez les devises pour les prix et les opérations. Définissez ci-dessous la valeur d'une unité dans la devise comptable de l'hôtel.|Emeza amafaranga akoreshwa ku biciro no mu bikorwa. Hasi shyiraho agaciro k'ifaranga rimwe mu ifaranga ry'ibaruramari rya hoteli.
Currency code|Code de devise|Kode y'ifaranga
Currency name|Nom de la devise|Izina ry'ifaranga
Add currency|Ajouter une devise|Ongeraho ifaranga
Account name|Nom du compte|Izina rya konti
Payment type|Type de paiement|Ubwoko bw'ubwishyu
Identifier type|Type d'identifiant|Ubwoko bw'ikiranga konti
Number or merchant code|Numéro ou code marchand|Nimero cyangwa kode y'umucuruzi
Add payment account|Ajouter un compte de paiement|Ongeraho konti yo kwishyuriraho
Payment account|Compte de paiement|Konti yo kwishyuriraho
Use 15–128 characters. Changing your password signs out existing sessions when their access tokens expire.|Utilisez 15 à 128 caractères. Les sessions existantes se termineront à l'expiration de leurs jetons d'accès.|Koresha inyuguti 15–128. Nyuma yo guhindura ijambo ry'ibanga, kwinjira gusanzwe guhagarara igihe impushya zo kwinjira zirangiye.
Current password|Mot de passe actuel|Ijambo ry'ibanga usanganywe
New password|Nouveau mot de passe|Ijambo ry'ibanga rishya
Confirm password|Confirmer le mot de passe|Emeza ijambo ry'ibanga
Open a valid reset link from your email. If this page was refreshed, reopen the link.|Ouvrez un lien valide reçu par e-mail. Si cette page a été actualisée, rouvrez le lien.|Fungura ihuza ryemewe woherejwe kuri imeyili. Niba wavuguruye uru rupapuro, ongera ufungure ihuza.
Back to sign in|Retour à la connexion|Subira ku kwinjira
Please wait…|Veuillez patienter…|Tegereza…
Send reset instructions|Envoyer les instructions|Ohereza amabwiriza
Save password|Enregistrer le mot de passe|Bika ijambo ry'ibanga
Hotel accounting currency|Devise comptable de l'hôtel|Ifaranga ry'ibaruramari rya hoteli
Select currency|Choisir une devise|Hitamo ifaranga
Select account|Choisir un compte|Hitamo konti
No configured account|Aucun compte configuré|Nta konti yashyizweho
Deactivate|Désactiver|Hagarika
Activate|Activer|Emeza
If an account exists for the supplied information, password reset instructions have been sent.|Si un compte correspond aux informations fournies, les instructions de réinitialisation ont été envoyées.|Niba konti ihuye n'amakuru watanze ibaho, amabwiriza yo guhindura ijambo ry'ibanga yoherejwe.
Password reset. You can now sign in.|Mot de passe réinitialisé. Vous pouvez vous connecter.|Ijambo ry'ibanga ryahinduwe. Ushobora kwinjira.
Use 15–128 characters and matching passwords.|Utilisez 15 à 128 caractères et des mots de passe identiques.|Koresha inyuguti 15–128 n'amagambo y'ibanga ahuye.
KBP Hotel — Hotel Management System|KBP Hotel — Système de gestion hôtelière|KBP Hotel — Sisitemu yo gucunga hoteli
Dashboard|Tableau de bord|Incamake
Overview|Vue d'ensemble|Incamake rusange
Rooms & Guests|Chambres et clients|Ibyumba n'abashyitsi
Food & Beverage|Restauration|Ibiribwa n'ibinyobwa
Inventory|Inventaire|Ububiko
Finance|Finances|Imari
Operations|Opérations|Ibikorwa
Administration|Administration|Ubuyobozi
Management System|Système de gestion|Sisitemu y'imicungire
Rooms|Chambres|Ibyumba
Room|Chambre|Icyumba
Room types|Types de chambres|Ubwoko bw'ibyumba
Room type|Type de chambre|Ubwoko bw'icyumba
Room number|Numéro de chambre|Nimero y'icyumba
Manage rooms|Gérer les chambres|Gucunga ibyumba
Manage room types|Gérer les types de chambres|Gucunga ubwoko bw'ibyumba
Reservations|Réservations|Kubika ibyumba
New reservation|Nouvelle réservation|Kubika icyumba
Choose dates first|Choisir les dates d'abord|Banza uhitemo amatariki
Check-in date|Date d'arrivée|Itariki yo kwinjira
Check-out date|Date de départ|Itariki yo gusohoka
Check availability|Vérifier la disponibilité|Reba ibyumba biboneka
Person or company|Personne ou entreprise|Umuntu cyangwa ikigo
Create customer or company|Créer un client ou une entreprise|Andika umukiriya cyangwa ikigo
Capacity|Capacité|Umubare w'abantu
Adults|Adultes|Abakuru
Children|Enfants|Abana
Preview reservation|Aperçu de la réservation|Reba amakuru yo kubika
Confirm reservation|Confirmer la réservation|Emeza kubika icyumba
Reservation confirmed|Réservation confirmée|Kubika icyumba byemejwe
No rooms available for these dates|Aucune chambre disponible à ces dates|Nta byumba biboneka kuri ayo matariki
Calendar start|Début du calendrier|Intangiriro y'ingengabihe
Select an available day to start a reservation|Choisissez un jour disponible pour réserver|Hitamo umunsi icyumba kiboneka kugira ngo ubike
Available|Disponible|Kiraboneka
Reserved|Réservée|Cyabitswe
Unavailable|Indisponible|Ntikiboneka
Dates|Dates|Amatariki
Cancel reservation|Annuler la réservation|Hagarika kubika icyumba
Check In|Arrivée|Kwinjira
Check Out|Départ|Gusohoka
Confirm check-in|Confirmer l'arrivée|Emeza kwinjira
Confirm check-out|Confirmer le départ|Emeza gusohoka
Guest Folios|Comptes clients|Konti z'abakiriya
Open customer folio|Ouvrir le compte client|Fungura konti y'umukiriya
Folio|Compte client|Konti y'umukiriya
Customer|Client|Umukiriya
Customers|Clients|Abakiriya
Customer type|Type de client|Ubwoko bw'umukiriya
Select customer|Choisir un client|Hitamo umukiriya
POS / Orders|Caisse / Commandes|Kugurisha / Ibyatumijwe
Product|Produit|Igicuruzwa
Select product|Choisir un produit|Hitamo igicuruzwa
Quantity|Quantité|Ingano
Item total|Total de l'article|Igiteranyo cy'igicuruzwa
Confirm item|Confirmer l'article|Emeza igicuruzwa
Cart|Panier|Ibyatoranyijwe
Grand total|Total général|Igiteranyo rusange
Remove|Retirer|Kuramo
Preview order|Aperçu de la commande|Reba ibyatumijwe
Confirm order for|Confirmer la commande pour|Emeza ibyo umukiriya atumije
Confirm and send|Confirmer et envoyer|Emeza wohereze
Order history|Historique des commandes|Amateka y'ibyatumijwe
Mark served|Marquer comme servi|Emeza ko byatanzwe
Void order|Annuler la commande|Hagarika ibyatumijwe
Order sent|Commande envoyée|Ibyatumijwe byoherejwe
Preparation queue|File de préparation|Ibyategurwa
Kitchen|Cuisine|Igikoni
Start preparation|Commencer la préparation|Tangira gutegura
Mark ready|Marquer comme prêt|Emeza ko byateguwe
Stock|Stock|Ububiko
Products and stock|Produits et stock|Ibicuruzwa n'ububiko
Stock movements|Mouvements de stock|Impinduka mu bubiko
Movement type|Type de mouvement|Ubwoko bw'impinduka
Confirm stock movement|Confirmer le mouvement|Emeza impinduka mu bubiko
Reason|Motif|Impamvu
Purchasing|Achats|Kugura
Vendor|Fournisseur|Umutanga w'ibicuruzwa
Vendors|Fournisseurs|Abatanga ibicuruzwa
Select vendor|Choisir un fournisseur|Hitamo utanga ibicuruzwa
Reference|Référence|Inomero iranga
Purchase unit price|Prix unitaire d'achat|Igiciro cyo kugura kimwe
Add item|Ajouter un article|Ongeramo igicuruzwa
Create purchase|Créer un achat|Andika igurwa
Receive goods|Réceptionner les marchandises|Akira ibicuruzwa
Pay vendor|Payer le fournisseur|Ishyura uwatanze ibicuruzwa
Total|Total|Igiteranyo
Paid|Payé|Byishyuwe
Balance|Solde|Asigaye
Actions|Actions|Ibikorwa
Amount|Montant|Amafaranga
Payment method|Mode de paiement|Uburyo bwo kwishyura
Confirm payment|Confirmer le paiement|Emeza ubwishyu
Cashier|Caissier|Umubitsi
Payment approvals|Approbations des paiements|Kwemeza ubwishyu
Approve|Approuver|Emeza
Reject|Rejeter|Wange
Transactions by payment method|Transactions par mode de paiement|Ubwishyu hakurikijwe uburyo
From|Du|Kuva
To|Au|Kugeza
Search|Rechercher|Shakisha
Refund|Rembourser|Subiza amafaranga
Payment covers|Objet du paiement|Ubwishyu bureba
Food and services|Restauration et services|Ibiribwa na serivisi
Room charges|Frais de chambre|Amafaranga y'icyumba
Split payment|Paiement fractionné|Kwishyura mu buryo butandukanye
Add payment method|Ajouter un mode de paiement|Ongeramo uburyo bwo kwishyura
Payment total|Total du paiement|Igiteranyo cy'ubwishyu
Remaining after approval|Solde après approbation|Asigaye nyuma yo kwemeza
Confirm payment for approval|Soumettre le paiement à approbation|Ohereza ubwishyu kugira ngo bwemezwe
Payment pending approval|Paiement en attente d'approbation|Ubwishyu butegereje kwemezwa
Credit Customers|Clients à crédit|Abakiriya bafite amadeni
View ledger|Voir le grand livre|Reba inyandiko z'imari
Approve credit|Approuver le crédit|Emeza ideni
Settle balance|Régler le solde|Ishyura asigaye
Credit ledger|Grand livre des crédits|Inyandiko z'amadeni
Reports|Rapports|Raporo
Report|Rapport|Raporo
Sales|Ventes|Ibyagurishijwe
Financial|Financier|Imari
Generate report|Générer le rapport|Kora raporo
Housekeeping|Entretien des chambres|Isuku y'ibyumba
Select room|Choisir une chambre|Hitamo icyumba
Assigned to|Assigné à|Ushinzwe
Unassigned|Non assigné|Ntawe uragenerwa
Date|Date|Itariki
Notes|Notes|Ibisobanuro
Create task|Créer une tâche|Tanga umurimo
Start task|Commencer la tâche|Tangira umurimo
Mark complete|Marquer comme terminé|Emeza ko byarangiye
Code|Code|Kode
Name|Nom|Izina
Full name|Nom complet|Amazina yose
Email|E-mail|Imeyili
Phone|Téléphone|Telefoni
Address|Adresse|Aderesi
Tax number|Numéro fiscal|Nimero y'imisoro
Active|Actif|Irakora
Payment terms (days)|Délai de paiement (jours)|Igihe cyo kwishyura (iminsi)
SKU|Référence produit|Kode y'igicuruzwa
Category|Catégorie|Icyiciro
Purchase unit|Unité d'achat|Igipimo cyo kugura
Selling unit|Unité de vente|Igipimo cyo kugurisha
Stock unit|Unité de stock|Igipimo cyo kubika
Stock units per purchase unit|Unités de stock par unité d'achat|Ibipimo byo kubika muri kimwe kiguzwe
Stock units per selling unit|Unités de stock par unité vendue|Ibipimo byo kubika muri kimwe kigurishijwe
Purchase price|Prix d'achat|Igiciro cyo kugura
Selling price|Prix de vente|Igiciro cyo kugurisha
Tax rate (0–1)|Taux de taxe (0–1)|Igipimo cy'umusoro (0–1)
Reorder level|Seuil de réapprovisionnement|Ingano yo kongera kugura
Destination|Destination|Aho bijya
Track stock|Suivre le stock|Kurikirana ububiko
Sellable|Vendable|Kiragurishwa
Purchasable|Achetable|Kiragurwa
Description|Description|Ibisobanuro
Standard occupancy|Occupation standard|Umubare usanzwe w'abantu
Maximum adults|Maximum d'adultes|Abakuru ntarengwa
Maximum children|Maximum d'enfants|Abana ntarengwa
Nightly rate|Tarif par nuit|Igiciro cy'ijoro
Floor|Étage|Igorofa
Beds|Lits|Ibitanda
Bed type|Type de lit|Ubwoko bw'igitanda
Bed dimensions|Dimensions du lit|Ingano y'igitanda
Create record|Créer une fiche|Andika amakuru
Select|Choisir|Hitamo
Save|Enregistrer|Bika
Saving|Enregistrement|Birabikwa
Saved|Enregistré|Byabitswe
Yes|Oui|Yego
No|Non|Oya
Details|Détails|Amakuru arambuye
No records found|Aucun enregistrement|Nta makuru yabonetse
Loading|Chargement|Birimo gufunguka
Status|Statut|Imiterere
Currency|Devise|Ifaranga
Type|Type|Ubwoko
Maintenance|Maintenance|Gusana
Issue|Problème|Ikibazo
Priority|Priorité|Icyihutirwa
Cancel|Annuler|Hagarika
Staff|Personnel|Abakozi
Initial password|Mot de passe initial|Ijambo ry'ibanga rya mbere
Language|Langue|Ururimi
All branches|Toutes les agences|Amashami yose
Role|Rôle|Inshingano
Assign role|Attribuer le rôle|Tanga inshingano
Remove role|Retirer le rôle|Kuraho inshingano
Suspend account|Suspendre le compte|Hagarika konti
Activate account|Activer le compte|Fungura konti
Audit Trail|Journal d'audit|Inyandiko z'igenzura
Action|Action|Igikorwa
Entity|Entité|Ikirebwa
User|Utilisateur|Umukoresha
Request|Requête|Icyifuzo
Settings|Paramètres|Igenamiterere
Legal name|Raison sociale|Izina ryemewe
Hotel name|Nom de l'hôtel|Izina rya hoteli
Time zone|Fuseau horaire|Igihe cy'akarere
Save settings|Enregistrer les paramètres|Bika igenamiterere
Expenses|Dépenses|Ibyasohotse
Record expense|Enregistrer la dépense|Andika amafaranga yasohotse
Expense ledger|Grand livre des dépenses|Inyandiko z'ibyashowe
Cashier shifts|Services de caisse|Ibihe by'akazi k'umubitsi
Opening float|Fonds de caisse initial|Amafaranga yo gutangirana
Open shift|Ouvrir le service|Tangira igihe cy'akazi
Counted cash|Espèces comptées|Amafaranga yabazwe
Close shift|Clôturer le service|Soza igihe cy'akazi
Expected cash|Espèces attendues|Amafaranga ateganyijwe
Difference|Écart|Ikinyuranyo
Reconcile|Rapprocher|Huza inyandiko z'imari
Rooms ready|Chambres prêtes|Ibyumba biteguye
Expected arrivals|Arrivées attendues|Abategerejwe kwinjira
Expected departures|Départs attendus|Abategerejwe gusohoka
Open orders|Commandes en cours|Ibyatumijwe bitararangira
Outstanding balance|Solde impayé|Amafaranga atarishyurwa
Pending payment approvals|Paiements à approuver|Ubwishyu bwo kwemeza
Sign out|Se déconnecter|Sohoka
Welcome back|Bon retour|Murakaza neza
Sign in to your staff account|Connectez-vous à votre compte|Injira muri konti yawe
Password|Mot de passe|Ijambo ry'ibanga
Enter your email|Saisissez votre e-mail|Andika imeyili yawe
Enter your password|Saisissez votre mot de passe|Andika ijambo ry'ibanga
Signing in...|Connexion...|Birimo kwinjira...
Sign In|Se connecter|Injira
Use the email and password provided by your administrator.|Utilisez les identifiants fournis par votre administrateur.|Koresha imeyili n'ijambo ry'ibanga wahawe n'umuyobozi.
Invalid email or password.|E-mail ou mot de passe incorrect.|Imeyili cyangwa ijambo ry'ibanga si byo.
An error occurred. Please try again.|Une erreur est survenue. Réessayez.|Habaye ikibazo. Ongera ugerageze.
Show password|Afficher le mot de passe|Erekana ijambo ry'ibanga
Hide password|Masquer le mot de passe|Hisha ijambo ry'ibanga
Platform administration|Administration de la plateforme|Ubuyobozi bwa sisitemu
Hotels and owners|Hôtels et propriétaires|Hoteli na ba nyirazo
Create a hotel and owner account|Créer un hôtel et son propriétaire|Andika hoteli na konti ya nyirayo
Unique hotel code|Code unique de l'hôtel|Kode yihariye ya hoteli
Owner full name|Nom complet du propriétaire|Amazina yose ya nyiri hoteli
Owner email|E-mail du propriétaire|Imeyili ya nyiri hoteli
Initial password (at least 15 characters)|Mot de passe initial (15 caractères minimum)|Ijambo ry'ibanga rya mbere (nibura inyuguti 15)
Create hotel and owner|Créer l'hôtel et le propriétaire|Andika hoteli na nyirayo
Creating…|Création…|Birimo gukorwa…
Registered hotels|Hôtels enregistrés|Hoteli zanditswe
No hotels registered.|Aucun hôtel enregistré.|Nta hoteli yanditswe.
Hotel|Hôtel|Hoteli
Unable to load hotels|Impossible de charger les hôtels|Ntibyashobotse gufungura hoteli
Unable to create hotel|Impossible de créer l'hôtel|Ntibyashobotse kwandika hoteli
Hotel, owner account, trial and main branch created. The owner can now sign in with the email and password you entered.|L'hôtel, le compte propriétaire, l'essai et l'agence principale sont créés. Le propriétaire peut se connecter avec les identifiants saisis.|Hoteli, konti ya nyirayo, igerageza n'ishami rikuru byakozwe. Nyiri hoteli ashobora kwinjira akoresheje imeyili n'ijambo ry'ibanga wanditse.
The request is invalid.|La demande est invalide.|Icyifuzo nticyemewe.
The operation conflicts with current state.|L'opération est incompatible avec l'état actuel.|Iki gikorwa ntigihuje n'imiterere iriho.
Access denied.|Accès refusé.|Ntiwemerewe.
An unexpected error occurred.|Une erreur inattendue est survenue.|Habaye ikibazo kitari giteganyijwe.
One or more fields are invalid.|Un ou plusieurs champs sont invalides.|Hari amakuru atanditswe neza.
The hotel API is unavailable. Please retry.|Le service hôtelier est indisponible. Réessayez.|Serivisi ya hoteli ntiboneka. Ongera ugerageze.
Session expired. Please sign in again.|Session expirée. Reconnectez-vous.|Igihe cyo gukoresha konti cyarangiye. Ongera winjire.
Sign in required|Connexion requise|Banza winjire
summary|Résumé|Incamake
subtotal|Sous-total|Igiteranyo mbere y'umusoro
tax|Taxe|Umusoro
total|Total|Igiteranyo
orders|Commandes|Ibyatumijwe
daily|Ventes quotidiennes|Ibyagurishijwe buri munsi
report_date|Date|Itariki
creditSales|Ventes à crédit|Ibyagurishijwe ku ideni
paymentsByMethod|Paiements par méthode|Ubwishyu hakurikijwe uburyo
ledgerActivity|Mouvements comptables|Impinduka mu nyandiko z'imari
vendorPayments|Paiements fournisseurs|Ubwishyu bw'abatanga ibicuruzwa
outstandingPurchases|Achats impayés|Ibyaguzwe bitarishyurwa
expenses|Dépenses|Ibyasohotse
method|Méthode|Uburyo
status|Statut|Imiterere
amount|Montant|Amafaranga
transactions|Transactions|Ubwishyu
kind|Type|Ubwoko
customer|Client|Umukiriya
vendor|Fournisseur|Utanga ibicuruzwa
reference|Référence|Inomero iranga
paid|Payé|Byishyuwe
balance|Solde|Asigaye
category|Catégorie|Icyiciro
created_at|Créé le|Byakozwe
housekeeping|Propreté|Isuku
operational|État opérationnel|Imiterere y'icyumba
id|Identifiant|Nimero iranga
KITCHEN|Cuisine|Igikoni
BAR|Bar|Akabari
SERVICE|Service|Serivisi
CASH|Espèces|Amafaranga mu ntoki
MOBILE_MONEY|Mobile money|Amafaranga kuri telefoni
CARD|Carte|Ikarita
BANK_TRANSFER|Virement bancaire|Kohereza kuri banki
CREDIT|Crédit|Ideni
INDIVIDUAL|Particulier|Umuntu ku giti cye
COMPANY|Entreprise|Ikigo
TOUR_AGENCY|Agence de voyage|Ikigo cy'ubukerarugendo
NGO|ONG|Umuryango utari uwa leta
GOVERNMENT|Administration publique|Ikigo cya leta
WALK_IN|Client de passage|Umukiriya utabanje kubika
DRAFT|Brouillon|Bitaremezwa
PENDING|En attente|Birategereje
CONFIRMED|Confirmé|Byemejwe
CHECKED_IN|Arrivé|Yarinjiye
CHECKED_OUT|Parti|Yarasohotse
CANCELLED|Annulé|Byahagaritswe
NO_SHOW|Absent|Ntiyaje
SENT|Envoyé|Byoherejwe
PREPARING|En préparation|Birategurwa
READY|Prêt|Byateguwe
SERVED|Servi|Byatanzwe
VOIDED|Annulé|Byateshejwe agaciro
POSTED|Comptabilisé|Byanditswe mu mari
REFUNDED|Remboursé|Amafaranga yasubijwe
APPROVED|Approuvé|Byemejwe
REJECTED|Rejeté|Byanzwe
RECEIVED|Réceptionné|Byakiriwe
OPEN|Ouvert|Birafunguye
CLOSED|Clôturé|Byarafunzwe
RECONCILED|Rapproché|Byahujwe
IN_PROGRESS|En cours|Birimo gukorwa
DONE|Terminé|Byarangiye
RESOLVED|Résolu|Byakemutse
ACTIVE|Actif|Irakora
SUSPENDED|Suspendu|Yahagaritswe
REVOKED|Révoqué|Yambuwe uburenganzira
LOW|Faible|Buto
MEDIUM|Moyenne|Buringaniye
HIGH|Élevée|Bukuru
URGENT|Urgente|Byihutirwa
CLEAN|Propre|Gisukuye
DIRTY|Sale|Kiranduye
INSPECTED|Inspecté|Cyagenzuwe
AVAILABLE|Disponible|Kiraboneka
OUT_OF_ORDER|Hors service|Ntigikora
OUT_OF_SERVICE|Indisponible|Ntigikoreshwa
OPENING|Stock initial|Ububiko bwo gutangira
ADJUSTMENT|Ajustement|Ikosora
WASTE|Perte|Ibyangiritse
RECEIPT|Réception|Ibyakiriwe
SALE|Vente|Ibyagurishijwe
REVERSAL|Contrepassation|Gusubiza inyuma
TRANSFER_IN|Transfert entrant|Ibyinjijwe bivuye ahandi
TRANSFER_OUT|Transfert sortant|Ibyoherejwe ahandi
CHARGE|Frais|Amafaranga asabwa
ACCOMMODATION|Hébergement|Icumbi
ORDER|Commande|Ibyatumijwe
PAYMENT|Paiement|Ubwishyu
REFUND|Remboursement|Gusubiza amafaranga
SUPER_ADMIN|Administrateur de plateforme|Umuyobozi mukuru wa sisitemu
OWNER|Propriétaire|Nyiri hoteli
MANAGER|Gérant|Umuyobozi
ACCOUNTANT|Comptable|Umubaruramari
RECEPTIONIST|Réceptionniste|Uwakira abashyitsi
CASHIER|Caissier|Umubitsi
WAITER|Serveur|Uhereza abakiriya
BARTENDER|Barman|Ushinzwe akabari
KITCHEN_STAFF|Personnel de cuisine|Umukozi w'igikoni
STOREKEEPER|Magasinier|Ushinzwe ububiko
HOUSEKEEPER|Agent d'entretien|Ushinzwe isuku
MAINTENANCE|Technicien|Ushinzwe gusana
AUDITOR|Auditeur|Umugenzuzi
SUPERVISOR|Superviseur|Umugenzuzi w'akazi
en|Anglais|Icyongereza
fr|Français|Igifaransa
rw|Kinyarwanda|Ikinyarwanda
Property management|Gestion de l’établissement|Gucunga hoteli
Rooms and room types|Chambres et catégories|Ibyumba n'ubwoko bwabyo
Manage room details, bed configurations and nightly prices.|Gérez les chambres, les lits et les tarifs par nuit.|Cunga ibyumba, ibitanda n'ibiciro by'ijoro.
Add room|Ajouter une chambre|Ongeramo icyumba
Add room type|Ajouter une catégorie|Ongeramo ubwoko bw'icyumba
Edit room|Modifier la chambre|Hindura icyumba
Delete room|Supprimer la chambre|Kuraho icyumba
Edit room type|Modifier la catégorie|Hindura ubwoko bw'icyumba
Delete room type|Supprimer la catégorie|Kuraho ubwoko bw'icyumba
Room details|Détails de la chambre|Amakuru y'icyumba
Room type details|Détails de la catégorie|Amakuru y'ubwoko bw'icyumba
Edit|Modifier|Hindura
Close|Fermer|Funga
The room type supplies the default price and bed details. Change the price for a room-specific rate.|La catégorie fournit le tarif et les détails des lits par défaut. Vous pouvez adapter le tarif de cette chambre.|Ubwoko bw'icyumba butanga igiciro n'amakuru y'ibitanda. Ushobora guhindura igiciro cy'iki cyumba.
Remove record|Supprimer la fiche|Kuraho amakuru
The record will be removed from active lists. Existing history will be preserved.|La fiche sera retirée des listes actives. Son historique sera conservé.|Aya makuru azava ku rutonde rukoreshwa. Amateka yayo azagumaho.
Select all roles this account needs. Permissions are combined.|Sélectionnez tous les rôles nécessaires. Les autorisations sont cumulées.|Hitamo inshingano zose iyi konti ikeneye. Uburenganzira burahuzwa.
Product details|Détails du produit|Amakuru y'igicuruzwa
Stock corrections are recorded as movements to preserve history.|Les corrections de stock sont enregistrées comme mouvements pour conserver l’historique.|Gukosora ububiko byandikwa nk'impinduka kugira ngo amateka abikwe.
One portfolio per customer, with all orders, stays and outstanding bills.|Un dossier par client, regroupant commandes, séjours et factures impayées.|Dosiye imwe kuri buri mukiriya, irimo ibyo yatumije, amacumbi n'amafaranga atarishyurwa.
Billing history|Historique de facturation|Amateka y'ibyishyuzwa
Menu and drinks|Carte et boissons|Amafunguro n'ibinyobwa
Manage menu items|Gérer les articles de la carte|Cunga amafunguro n'ibinyobwa
Finance and reports|Finances et rapports|Imari na raporo
Review payments, manage your shift and reconcile collections.|Vérifiez les paiements, gérez votre caisse et rapprochez les encaissements.|Suzuma ubwishyu, cunga igihe cyawe cy'akazi kandi ugenzure amafaranga yakiriwe.
Owner self-approval requires a reason.|Le propriétaire doit justifier l’approbation de son propre paiement.|Nyiri hoteli agomba gutanga impamvu yo kwemeza ubwishyu yanditse.
A different user must approve this payment.|Un autre utilisateur doit approuver ce paiement.|Undi mukozi agomba kwemeza ubu bwishyu.
Payment decision|Décision de paiement|Icyemezo ku bwishyu
Approve payment|Approuver le paiement|Emeza ubwishyu
Reject payment|Rejeter le paiement|Anga ubwishyu
Payment decisions are recorded in the audit trail.|Les décisions sont enregistrées dans le journal d’audit.|Ibyemezo ku bwishyu byandikwa mu mateka y'igenzura.
Confirm|Confirmer|Emeza
Menu management|Gestion de la carte|Gucunga amafunguro n'ibinyobwa
Create dishes, drinks and services for the POS menu.|Ajoutez des plats, boissons et services à la carte de vente.|Ongeramo amafunguro, ibinyobwa na serivisi ku rutonde rwo kugurisha.
Add menu item|Ajouter un article à la carte|Ongeramo ifunguro cyangwa ikinyobwa
Tax percent|Taxe en pourcentage|Ijanisha ry'umusoro
Track finished-item stock|Suivre le stock des articles finis|Kurikirana ububiko bw'ibicuruzwa byuzuye
Non-stock item|Article sans suivi de stock|Igicuruzwa kidakurikiranwa mu bubiko
Daily sales|Ventes quotidiennes|Ibyagurishijwe buri munsi
Payment methods|Modes de paiement|Uburyo bwo kwishyura
Operating expenses|Charges d’exploitation|Amafaranga akoreshwa mu mirimo
Purchase register|Registre des achats|Urutonde rw'ibyaguzwe
Supplier payments|Paiements fournisseurs|Ubwishyu bw'abatanga ibicuruzwa
Customer balances|Soldes clients|Amafaranga abakiriya basigaje kwishyura
Supplier balances|Soldes fournisseurs|Amafaranga abatanga ibicuruzwa basigaje kwishyurwa
All bill activity|Toutes les opérations de facturation|Ibyakozwe byose ku byishyuzwa
Cashier reconciliation|Rapprochement de caisse|Kugenzura amafaranga yo mu isanduku
Report period|Période du rapport|Igihe cya raporo
Summary|Synthèse|Incamake
Understand sales, cash movement and outstanding balances.|Consultez les ventes, les flux de trésorerie et les soldes impayés.|Reba ibyagurishijwe, uko amafaranga yinjiye n'ayasohotse, hamwe n'atarishyurwa.
Export CSV|Exporter en CSV|Sohora CSV
Print / Save PDF|Imprimer / Enregistrer en PDF|Capa / Bika nka PDF
Net cash movement is receipts minus refunds, supplier payments and expense payments. It is not profit or a bank balance.|Le flux net correspond aux encaissements moins remboursements, paiements fournisseurs et dépenses payées. Ce n’est ni un bénéfice ni un solde bancaire.|Impinduka y'amafaranga ni ayakiriwe ukuyemo ayasubijwe, ayishyuwe abatanga ibicuruzwa n'ayakoreshejwe. Si inyungu cyangwa amafaranga ari kuri banki.
Current outstanding balances at report generation time.|Soldes impayés au moment de la génération du rapport.|Amafaranga atarishyurwa igihe raporo yakozwe.
Billed revenue|Montant facturé|Amafaranga yishyujwe
Customer receipts|Encaissements clients|Amafaranga yakiriwe ku bakiriya
Refunds|Remboursements|Amafaranga yasubijwe
Net cash movement|Flux net de trésorerie|Impinduka y'amafaranga
Net receipts|Encaissements nets|Amafaranga yakiriwe asigaye
Awaiting approval amount|Montant en attente d’approbation|Amafaranga ategereje kwemezwa
Approved payments|Paiements approuvés|Ubwishyu bwemejwe
Folio status|Statut du compte client|Uko konti y'umukiriya ihagaze
Opened|Ouvert le|Igihe yafunguriwe
Closed|Fermé le|Igihe yafungiwe
Customer credit|Crédit clients|Amadeni y'abakiriya
Bills|Factures et additions|Ibyishyuzwa
Open your cashier shift before approving a payment.|Ouvrez votre caisse avant d’approuver un paiement.|Banza ufungure igihe cyawe cyo kwakira amafaranga mbere yo kwemeza ubwishyu.
Owner approval of their own payment requires a reason.|Le propriétaire doit justifier l’approbation de son propre paiement.|Nyiri hoteli agomba gutanga impamvu yo kwemeza ubwishyu yanditse.
billedRevenue|Montant facturé|Amafaranga yishyujwe
receipts|Encaissements clients|Amafaranga yakiriwe
refunds|Remboursements|Amafaranga yasubijwe
supplierPayments|Paiements fournisseurs|Ubwishyu bw'abatanga ibicuruzwa
expensePayments|Dépenses payées|Amafaranga yishyuwe mu mirimo
operatingExpenses|Charges d’exploitation|Amafaranga akoreshwa mu mirimo
netCashMovement|Flux net de trésorerie|Impinduka y'amafaranga
Expense payments|Dépenses payées|Amafaranga yishyuwe mu mirimo
Approval status|Statut d'approbation|Uko kwemeza bihagaze
Device|Appareil|Igikoresho
These are outstanding purchase orders, not matched supplier invoices.|Ces soldes concernent les commandes, pas des factures fournisseurs rapprochées.|Aya ni amafaranga asigaye ku byatumijwe, si fagitire zagenzuwe z'abatanga ibicuruzwa.
balancesAt|Soldes calculés le|Igihe amafaranga asigaye yabariwe
from|Du|Kuva
to|Au|Kugeza
Creating the hotel starts its three-month trial and creates the first hotel owner account.|La création de l'hôtel démarre sa période d'essai de trois mois et crée le premier compte propriétaire de l'hôtel.|Kwandikisha hoteli bitangiza igerageza ry'amezi atatu kandi bigakora konti ya mbere ya nyir'hoteli.
Hotel settings|Paramètres de l'hôtel|Igenamiterere rya hoteli
Manage the hotel's identity and operating defaults.|Gérez l'identité de l'hôtel et ses paramètres opérationnels par défaut.|Genzura umwirondoro wa hoteli n'igenamiterere ry'ibanze ry'imikorere.
Accepted currencies and exchange rates|Devises acceptées et taux de change|Amafaranga yemewe n'ibipimo by'ivunjisha
Exchange rates convert foreign customer payments into the hotel's base currency.|Les taux de change convertissent les paiements des clients en devises étrangères dans la devise de base de l'hôtel.|Ibipimo by'ivunjisha bihindura ubwishyu bw'abakiriya mu mafaranga y'amahanga bukajya mu ifaranga ry'ibanze rya hoteli.
Hotel base currency|Devise de base de l'hôtel|Ifaranga ry'ibanze rya hoteli
Add exchange rate|Ajouter un taux de change|Ongeraho igipimo cy'ivunjisha
Foreign currency|Devise étrangère|Ifaranga ry'amahanga
Rate to base currency|Taux vers la devise de base|Igipimo kijya mu ifaranga ry'ibanze
Effective from|Applicable à partir de|Gitangira gukurikizwa
Payment rate preview|Aperçu du taux de paiement|Igaragaza ry'igipimo cy'ubwishyu
Save exchange rate|Enregistrer le taux de change|Bika igipimo cy'ivunjisha
Exchange rate saved successfully.|Taux de change enregistré avec succès.|Igipimo cy'ivunjisha cyabitswe neza.
Current exchange rates|Taux de change actuels|Ibipimo by'ivunjisha biriho
Exchange rate|Taux de change|Igipimo cy'ivunjisha
No foreign exchange rates configured.|Aucun taux de change étranger n'est configuré.|Nta gipimo cy'ivunjisha ry'amahanga cyashyizweho.
Exchange rate history|Historique des taux de change|Amateka y'ibipimo by'ivunjisha
Created|Créé|Byakozwe
Active|Actif|Kirakora
Inactive|Inactif|Ntigikora
Payment currency|Devise de paiement|Ifaranga ryo kwishyura
Foreign-currency payments use the hotel's configured exchange rate.|Les paiements en devises étrangères utilisent le taux de change configuré par l'hôtel.|Ubwishyu mu mafaranga y'amahanga bukoresha igipimo cy'ivunjisha cyashyizweho na hoteli.
Enter a three-letter currency code.|Saisissez un code de devise à trois lettres.|Andika kode y'ifaranga igizwe n'inyuguti eshatu.
Calculating exchange rate...|Calcul du taux de change...|Kubara igipimo cy'ivunjisha...
Payment equivalent|Équivalent du paiement|Agaciro k'ubwishyu mu ifaranga ry'ibanze
Payment total in base currency|Total du paiement dans la devise de base|Igiteranyo cy'ubwishyu mu ifaranga ry'ibanze
Unable to calculate exchange rate.|Impossible de calculer le taux de change.|Ntibishobotse kubara igipimo cy'ivunjisha.
Unable to submit payment.|Impossible de soumettre le paiement.|Ntibishobotse kohereza ubwishyu.
Unable to load folio.|Impossible de charger le compte client.|Ntibishobotse gufungura konti y'umukiriya.
Total to apply|Total à appliquer|Igiteranyo kigomba kwishyurwa
Unable to load payment approvals.|Impossible de charger les approbations de paiement.|Ntibishobotse gufungura ubwishyu butegereje kwemezwa.
Unable to load transactions.|Impossible de charger les transactions.|Ntibishobotse gufungura ibikorwa by'ubwishyu.
Unable to complete payment decision.|Impossible de terminer la décision de paiement.|Ntibishobotse kurangiza icyemezo cy'ubwishyu.
Current stock|Stock actuel|Ububiko buriho
Record stock movement|Enregistrer un mouvement de stock|Andika impinduka y'ububiko
Movement history|Historique des mouvements|Amateka y'impinduka z'ububiko
No stock movements yet.|Aucun mouvement de stock pour le moment.|Nta mpinduka y'ububiko irandikwa.
is required.|est obligatoire.|irasabwa.
must be a valid number.|doit être un nombre valide.|igomba kuba umubare wemewe.
must be at least|doit être au moins|igomba nibura kuba
must be a valid email address.|doit être une adresse e-mail valide.|igomba kuba aderesi ya imeyili yemewe.
must not be greater than|ne doit pas être supérieur à|ntigomba kurenza
Edit product|Modifier le produit|Hindura igicuruzwa
Record operating expense|Enregistrer une dépense d’exploitation|Andika amafaranga yakoreshejwe mu mirimo
Choose the expense category configured by hotel management.|Choisissez la catégorie de dépense configurée par la direction de l’hôtel.|Hitamo icyiciro cy'amafaranga cyashyizweho n'ubuyobozi bwa hoteli.
Select expense category|Sélectionner une catégorie de dépense|Hitamo icyiciro cy'amafaranga
Expense categories|Catégories de dépenses|Ibyiciro by'amafaranga akoreshwa
Create and maintain the categories cashiers can use when recording expenses.|Créez et gérez les catégories que les caissiers peuvent utiliser pour enregistrer les dépenses.|Kora kandi ucunge ibyiciro abashinzwe kwakira amafaranga bakoresha bandika amafaranga yakoreshejwe.
New category|Nouvelle catégorie|Icyiciro gishya
Category name|Nom de la catégorie|Izina ry'icyiciro
Add category|Ajouter la catégorie|Ongeraho icyiciro
Edit category|Modifier la catégorie|Hindura icyiciro
Save changes|Enregistrer les modifications|Bika impinduka
No active expense categories are available.|Aucune catégorie de dépense active n’est disponible.|Nta cyiciro cy'amafaranga gikora gihari.
No expense categories configured.|Aucune catégorie de dépense n’est configurée.|Nta cyiciro cy'amafaranga cyashyizweho.
Inactive categories remain in historical expenses but cannot be selected for new expenses.|Les catégories inactives restent dans l’historique mais ne peuvent pas être utilisées pour de nouvelles dépenses.|Ibyiciro bidakora biguma mu mateka y'amafaranga ariko ntibishobora gukoreshwa ku mafaranga mashya.
Expense recorded successfully.|Dépense enregistrée avec succès.|Amafaranga yakoreshejwe yanditswe neza.
Expense category created successfully.|Catégorie de dépense créée avec succès.|Icyiciro cy'amafaranga cyakozwe neza.
Expense category updated successfully.|Catégorie de dépense mise à jour avec succès.|Icyiciro cy'amafaranga cyahinduwe neza.
Expense date is required.|La date de la dépense est obligatoire.|Itariki y'amafaranga yakoreshejwe irasabwa.
Expense category is required.|La catégorie de dépense est obligatoire.|Icyiciro cy'amafaranga kirasabwa.
Amount must be greater than zero.|Le montant doit être supérieur à zéro.|Amafaranga agomba kuba arenga zeru.
Amount is required.|Le montant est obligatoire.|Amafaranga arasabwa.
Description is required.|La description est obligatoire.|Ibisobanuro birasabwa.
Description must not exceed 1000 characters.|La description ne doit pas dépasser 1 000 caractères.|Ibisobanuro ntibigomba kurenza inyuguti 1000.
Payment method is required.|Le mode de paiement est obligatoire.|Uburyo bwo kwishyura burasabwa.
Choose a valid expense payment method.|Choisissez un mode de paiement valide pour la dépense.|Hitamo uburyo bwemewe bwo kwishyura aya mafaranga.
Request identifier is required.|L’identifiant de la demande est obligatoire.|Nimero iranga iki gikorwa irasabwa.
Select an expense category.|Sélectionnez une catégorie de dépense.|Hitamo icyiciro cy'amafaranga.
Select an active expense category.|Sélectionnez une catégorie de dépense active.|Hitamo icyiciro cy'amafaranga gikora.
Category name is required.|Le nom de la catégorie est obligatoire.|Izina ry'icyiciro rirasabwa.
Category name must not exceed 100 characters.|Le nom de la catégorie ne doit pas dépasser 100 caractères.|Izina ry'icyiciro ntirigomba kurenza inyuguti 100.
Category status is required.|Le statut de la catégorie est obligatoire.|Imiterere y'icyiciro irasabwa.
An expense category with this name already exists.|Une catégorie de dépense portant ce nom existe déjà.|Icyiciro cy'amafaranga gifite iri zina gisanzwe gihari.
Amount can have at most 15 integer digits and 4 decimal places.|Le montant peut contenir au maximum 15 chiffres entiers et 4 décimales.|Amafaranga ashobora kugira imibare 15 mbere y'akadomo n'imibare 4 nyuma yako.
Choose what this payment covers.|Choisissez ce que ce paiement couvre.|Hitamo icyo ubu bwishyu bugenewe.
Checked-in guest folio|Folio du client hébergé|Konti y'umukiriya ucumbitse
Post charges to a guest staying in the hotel.|Ajouter les frais au compte d'un client hébergé.|Shyira amafaranga kuri konti y'umukiriya ucumbitse.
Non-resident bill|Facture client non-résident|Fagitire y'umukiriya udacumbitse
Walk-in and outside customers. No room number required.|Clients de passage et externes. Aucun numéro de chambre requis.|Abakiriya batacumbitse. Nta nimero y'icyumba ikenewe.
Guest billing|Facturation client hébergé|Kwishyuza umukiriya ucumbitse
Choose the customer who is currently checked in.|Choisissez le client actuellement enregistré à l'hôtel.|Hitamo umukiriya uri muri hoteli ubu.
Select checked-in guest|Sélectionner un client hébergé|Hitamo umukiriya ucumbitse
The backend verifies that the selected customer has an active checked-in stay.|Le système vérifie que le client sélectionné est actuellement hébergé.|Sisitemu igenzura ko umukiriya wahisemo ari muri hoteli.
Non-resident bill setup|Configuration de la facture non-résident|Gutegura fagitire y'udacumbitse
Use a walk-in customer or attach the bill to an existing customer or organization.|Utilisez un client de passage ou associez la facture à un client ou une organisation existante.|Koresha umukiriya winjiye ako kanya cyangwa uhuze fagitire n'umukiriya cyangwa ikigo gisanzwe.
Start new bill|Nouvelle facture|Tangira fagitire nshya
Active non-resident bill|Facture non-résident active|Fagitire y'udacumbitse iri gukora
Open bill|Ouvrir la facture|Fungura fagitire
Bill type|Type de facture|Ubwoko bwa fagitire
Walk-in customer|Client de passage|Umukiriya winjiye ako kanya
Table / reference|Table / référence|Ameza / inomero
Notes|Notes|Ibisobanuro
Optional notes|Notes facultatives|Ibisobanuro bitari ngombwa
Menu items|Articles du menu|Ibiri kuri menu
Tap an item to add it to the order.|Cliquez sur un article pour l'ajouter à la commande.|Kanda ku kintu kugira ngo ugishyire ku itegeko.
Search menu items|Rechercher dans le menu|Shakisha kuri menu
No menu items match your search.|Aucun article ne correspond à votre recherche.|Nta kintu kuri menu gihuye n'ishakisha.
Order summary|Résumé de la commande|Incamake y'itegeko
items|articles|ibintu
Non-resident|Non-résident|Udacumbitse
Guest folio|Folio client|Konti y'umukiriya
Your order is empty|Votre commande est vide|Itegeko nta kintu ririmo
Select menu items to begin.|Sélectionnez des articles pour commencer.|Hitamo ibintu kuri menu kugira ngo utangire.
Subtotal|Sous-total|Igiteranyo mbere y'umusoro
This bill will use the hotel walk-in customer.|Cette facture utilisera le client de passage de l'hôtel.|Iyi fagitire izakoresha umukiriya usanzwe winjira ako kanya.
Order sent to the non-resident bill.|Commande ajoutée à la facture non-résident.|Itegeko ryashyizwe kuri fagitire y'udacumbitse.
Order sent to the guest folio.|Commande ajoutée au folio du client.|Itegeko ryashyizwe kuri konti y'umukiriya.
Open folio and record payment|Ouvrir le folio et enregistrer le paiement|Fungura konti wandike ubwishyu
View charges, payments and settle the customer balance.|Consultez les frais et paiements et réglez le solde du client.|Reba amafaranga n'ubwishyu kandi wishyure asigaye.
Back to folios|Retour aux folios|Subira kuri konti
The folio has an unpaid balance. Settle or move the approved balance to credit before closing it.|Le folio présente un solde impayé. Réglez-le ou transférez le solde approuvé au crédit avant de le clôturer.|Konti iracyafite umwenda. Wishyure cyangwa amafaranga yemejwe ashyirwe ku ideni mbere yo kuyifunga.
Total charges|Total des frais|Amafaranga yose yishyuzwa
Paid amount|Montant payé|Amafaranga yishyuwe
Remaining amount|Montant restant|Amafaranga asigaye
Folio status|Statut du folio|Imiterere ya konti
PARTIAL|Partiel|Igice cyishyuwe
Folio ledger|Grand livre du folio|Inyandiko za konti
All charges, payments, refunds and reversals.|Tous les frais, paiements, remboursements et annulations.|Amafaranga yose, ubwishyu, gusubizwa n'ibyasubijwe inyuma.
Date & time|Date et heure|Itariki n'isaha
No folio activity yet.|Aucune activité sur le folio.|Nta gikorwa kiraba kuri konti.
Record payment|Enregistrer un paiement|Andika ubwishyu
Full or partial settlement|Paiement total ou partiel|Kwishyura byose cyangwa igice
Currency|Devise|Ifaranga
Folio settled|Folio soldé|Konti yarishyuwe
There is no outstanding balance to collect.|Il n'y a aucun solde impayé à encaisser.|Nta mafaranga asigaye kwishyurwa.
RESTAURANT|Restaurant|Resitora
LAUNDRY|Blanchisserie|Kumesera
TRANSPORT|Transport|Ubwikorezi
SWIMMING_POOL|Piscine|Pisine
DAY_USE|Utilisation journalière|Serivisi y'umunsi
EVENT|Événement|Ibirori
CONFERENCE|Conférence|Inama
OUTSIDE_CATERING|Service traiteur externe|Serivisi y'ibiryo hanze
OTHER|Autre|Ibindi
All|Tous|Byose
Tax|Taxe|Umusoro
Choose a checked-in guest folio or open a non-resident bill.|Choisissez le folio d'un client enregistré ou ouvrez une facture pour un client non-résident.|Hitamo konti y'umukiriya ucumbitse cyangwa ufungure fagitire y'umukiriya udacumbitse.
Sales and purchasing|Ventes et achats|Kugurisha no kugura
Open Sales and purchasing for invoices, receiving, payments and financial reports. Your administrator must enable the required modules.|Ouvrez Ventes et achats pour les factures, réceptions, paiements et rapports financiers. Votre administrateur doit activer les modules requis.|Fungura Kugurisha no kugura urebe fagitire, kwakira ibicuruzwa, kwishyura na raporo z'imari. Umuyobozi agomba gufungura ibice bikenewe.
Point of sale|Point de vente|Aho kugurishiriza
Purchases|Achats|Ibyaguzwe
Sales history|Historique des ventes|Amateka y'ibyagurishijwe
Customers and suppliers|Clients et fournisseurs|Abakiriya n'abatanga ibicuruzwa
Payment accounts|Comptes de paiement|Konti zo kwishyuriraho
Cashier shifts|Services des caissiers|Ibihe by'akazi by'abakira amafaranga
Financial reports|Rapports financiers|Raporo z'imari
Operating expenses|Charges d'exploitation|Amafaranga akoreshwa mu kazi
This operation is not available for your role or enabled modules.|Cette opération n'est pas disponible pour votre rôle ou les modules activés.|Iki gikorwa nticyemerewe inshingano zawe cyangwa ibice byafunguwe.
The transaction conflicts with current stock, credit, shift or request state. Review history before retrying.|La transaction est incompatible avec le stock, le crédit, le service ou la requête. Consultez l'historique avant de réessayer.|Igikorwa ntigihuye n'ububiko, ideni, igihe cy'akazi cyangwa ubusabe. Banza urebe amateka mbere yo kongera kugerageza.
Unable to complete the transaction. Check the entered amounts and required fields.|Impossible de terminer la transaction. Vérifiez les montants et les champs requis.|Igikorwa nticyarangiye. Reba amafaranga wanditse n'ibisabwa byose.
A transaction is awaiting confirmation. Retry the same request to avoid duplicate posting.|Une transaction attend confirmation. Réessayez la même requête pour éviter un double enregistrement.|Igikorwa kiracyategereje kwemezwa. Ongera wohereze ubusabe bumwe wirinde kubwandika kabiri.
Retry transaction|Réessayer la transaction|Ongera ugerageze igikorwa
Opening inventory approval|Approbation du stock initial|Kwemeza ububiko bw'ibanze
Review current quantities and purchase costs before approving opening inventory valuation. The base currency is locked after approval.|Vérifiez les quantités et coûts d'achat avant d'approuver la valorisation initiale. La devise de base sera ensuite verrouillée.|Reba ingano n'ibiciro by'ibyaguzwe mbere yo kwemeza agaciro k'ububiko bw'ibanze. Ifaranga ry'ibanze ntirizongera guhindurwa.
Approve opening inventory valuation|Approuver la valorisation initiale|Emeza agaciro k'ububiko bw'ibanze
Inventory valuation|Valorisation du stock|Agaciro k'ibicuruzwa mu bubiko
Select a branch and stock location first.|Sélectionnez d'abord une agence et un emplacement de stock.|Banza uhitemo ishami n'aho ububiko buri.
Search or scan products|Rechercher ou scanner des produits|Shaka cyangwa sikana ibicuruzwa
Add product|Ajouter un produit|Ongeramo igicuruzwa
Price override (optional)|Prix dérogatoire (facultatif)|Igiciro cyihariye (si ngombwa)
Review invoice|Vérifier la facture|Suzuma fagitire
Leave payment empty only for approved customer credit or unpaid supplier invoices.|Laissez le paiement vide uniquement pour un crédit client approuvé ou une facture fournisseur impayée.|Ntuzuzuze ubwishyu gusa igihe ideni ry'umukiriya ryemejwe cyangwa fagitire y'utanga ibicuruzwa itarishyurwa.
Cashier shift|Service du caissier|Igihe cy'akazi cy'uwakira amafaranga
Post invoice|Comptabiliser la facture|Emeza fagitire mu bitabo
Credit limit in base currency|Plafond de crédit en devise de base|Ideni ntarengwa mu ifaranga ry'ibanze
Account number, mobile number or merchant code|Numéro de compte, téléphone ou code marchand|Nimero ya konti, telefone cyangwa kode y'umucuruzi
Sales receipt|Reçu de vente|Inyemezabwishyu y'ibyagurishijwe
Purchase invoice|Facture d'achat|Fagitire y'ibyaguzwe
Outstanding|Solde restant|Asigaye kwishyurwa
Business receipt. Not an official Rwanda EBM invoice.|Reçu commercial. Ce document n'est pas une facture EBM officielle du Rwanda.|Inyemezabwishyu y'ubucuruzi. Si fagitire ya EBM yemewe mu Rwanda.
Receipt format|Format du reçu|Imiterere y'inyemezabwishyu
Print or save PDF|Imprimer ou enregistrer en PDF|Sohora cyangwa ubike PDF
Returned|Retourné|Ibyasubijwe
Quantity received|Quantité reçue|Ingano yakiriwe
Receive goods|Réceptionner les marchandises|Akira ibicuruzwa
Return quantity|Quantité retournée|Ingano isubizwa
Return to saleable stock|Réintégrer le stock vendable|Subiza mu bubiko bw'ibishobora kugurishwa
Record return|Enregistrer le retour|Andika ibisubijwe
Cash account|Compte de caisse|Konti y'amafaranga mu ntoki
Opening cash|Fonds de caisse initial|Amafaranga ahari mu gutangira
Open shift|Ouvrir le service|Tangira igihe cy'akazi
Expected cash|Encaisse attendue|Amafaranga ateganyijwe
Variance|Écart|Ikinyuranyo
Counted cash|Espèces comptées|Amafaranga yabazwe
Close shift|Clôturer le service|Soza igihe cy'akazi
Cash deposit or withdrawal|Dépôt ou retrait de caisse|Kwinjiza cyangwa gusohora amafaranga
Record cash movement|Enregistrer le mouvement de caisse|Andika iyinjira cyangwa isohoka ry'amafaranga
Approve shift|Approuver le service|Emeza igihe cy'akazi
Record expense|Enregistrer la charge|Andika amafaranga yakoreshejwe
Generate report|Générer le rapport|Kora raporo
Journal amounts use base currency. Inventory shows current balances; journal totals use the selected dates.|Les écritures utilisent la devise de base. Le stock indique les soldes actuels ; le journal couvre les dates choisies.|Amafaranga mu bitabo ni mu ifaranga ry'ibanze. Ububiko bwerekana ibisigaye ubu; ibitabo byerekana amatariki yahiswemo.
Account balances|Soldes des comptes|Amafaranga asigaye kuri konti
Sales and purchases|Ventes et achats|Ibyagurishijwe n'ibyaguzwe
Collections|Encaissements|Amafaranga yakiriwe
Commerce modules|Modules Commerce|Ibice by'ubucuruzi
Core and inventory remain enabled. Retail requires accounting.|Le socle et le stock restent activés. La vente au détail nécessite la comptabilité.|Ibice by'ibanze n'ububiko biguma bifunguye. Kugurisha ku giti bisaba ibaruramari.
Manage modules|Gérer les modules|Genzura ibice bya sisitemu
Unable to save module access.|Impossible d'enregistrer les accès aux modules.|Uburenganzira bw'ibice ntibwabitswe.
Module access updated. Tenant permissions remain unchanged.|Accès aux modules mis à jour. Les permissions de l'organisation sont inchangées.|Uburenganzira bw'ibice bwavuguruwe. Uburenganzira bw'ikigo ntibwahindutse.
Edit product and pricing|Modifier le produit et les prix|Hindura igicuruzwa n'ibiciro
Unable to update product. Check permissions, prices and required fields.|Impossible de modifier le produit. Vérifiez les permissions, les prix et les champs requis.|Igicuruzwa nticyahinduwe. Reba uburenganzira, ibiciro n'ibisabwa.
Minimum selling price|Prix de vente minimum|Igiciro cyo hasi cyo kugurisha
Tax rate (%)|Taux de taxe (%)|Igipimo cy'umusoro (%)
Low stock threshold|Seuil de stock faible|Ingano nto ntarengwa mu bubiko
Tax inclusive|Taxe incluse|Umusoro urimo
Recoverable purchase tax|Taxe d'achat récupérable|Umusoro w'ibyaguzwe ushobora gusubizwa
Branch selling price|Prix de vente de l'agence|Igiciro cyo kugurisha cy'ishami
Save branch price|Enregistrer le prix de l'agence|Bika igiciro cy'ishami
Base units per package|Unités de base par emballage|Ibipimo by'ibanze muri buri paki
Add unit|Ajouter une unité|Ongeramo igipimo
Supplier|Fournisseur|Utanga ibicuruzwa
Unit|Unité|Igipimo
Unit price|Prix unitaire|Igiciro cya kimwe
Due date|Date d'échéance|Itariki ntarengwa yo kwishyura
Add payment|Ajouter un paiement|Ongeramo ubwishyu
Received|Reçu|Ibyakiriwe
Approved|Approuvé|Byemejwe
Gross profit|Bénéfice brut|Inyungu mbisi
Customer and supplier balances|Soldes clients et fournisseurs|Amafaranga asigaye ku bakiriya n'abatanga ibicuruzwa
Product sales before returns|Ventes par produit avant retours|Ibyagurishijwe kuri buri gicuruzwa mbere y'ibisubijwe
Returns|Retours|Ibisubijwe
Edit customer or supplier|Modifier le client ou fournisseur|Hindura umukiriya cyangwa utanga ibicuruzwa
CUSTOMER|Client|Umukiriya
SUPPLIER|Fournisseur|Utanga ibicuruzwa
branch id|Identifiant de l'agence|Ikiranga ishami
branch|Agence|Ishami
account|Compte|Konti
debit|Débit|Debi
credit|Crédit|Kredi
balance|Solde|Asigaye
location|Emplacement|Ahantu
sku|Référence produit|Kode y'igicuruzwa
name|Nom|Izina
quantity|Quantité|Ingano
inventory value|Valeur du stock|Agaciro k'ububiko
low stock quantity|Seuil de stock|Ingano ntarengwa mu bubiko
low stock|Stock faible|Ububiko buke
created by|Créé par|Byanditswe na
party name|Client ou fournisseur|Umukiriya cyangwa utanga ibicuruzwa
currency|Devise|Ifaranga
kind|Type|Ubwoko
documents|Documents|Inyandiko
total|Total|Igiteranyo
base total|Total en devise de base|Igiteranyo mu ifaranga ry'ibanze
method|Méthode|Uburyo
refund|Remboursement|Gusubiza amafaranga
amount|Montant|Amafaranga
base amount|Montant en devise de base|Amafaranga mu ifaranga ry'ibanze
document number|Numéro de document|Nimero y'inyandiko
due date|Échéance|Itariki ntarengwa
outstanding|Solde restant|Asigaye kwishyurwa
description|Description|Ibisobanuro
category name|Catégorie|Icyiciro
revenue|Chiffre d'affaires|Amafaranga y'ibyagurishijwe
cost|Coût|Ikiguzi
gross profit|Bénéfice brut|Inyungu mbisi
margin percent|Marge (%)|Igipimo cy'inyungu (%)
document amount|Montant du document|Amafaranga y'inyandiko
base net|Net en devise de base|Amafaranga adafite umusoro mu ifaranga ry'ibanze
base tax|Taxe en devise de base|Umusoro mu ifaranga ry'ibanze
reason|Motif|Impamvu
created at|Date de création|Itariki byandikiwe
Roles|Rôles|Inshingano
Staff roles (select more than one)|Rôles du personnel (plusieurs choix possibles)|Inshingano z’abakozi (ushobora guhitamo zirenze imwe)
Create product|Créer un produit|Ongeraho igicuruzwa
Stock operations|Opérations de stock|Ibikorwa by’ububiko
Select registered product|Sélectionner un produit enregistré|Hitamo igicuruzwa cyanditswe
Add product from catalog|Ajouter un produit du catalogue|Ongeraho igicuruzwa kiri ku rutonde
Print receipt|Imprimer le reçu|Sohora inyemezabwishyu
Print invoice|Imprimer la facture|Sohora inyemezabuguzi
Choose every job this person performs. OWNER access cannot be assigned to employees.|Sélectionnez toutes les fonctions exercées par cette personne. Le rôle PROPRIÉTAIRE ne peut pas être attribué aux employés.|Hitamo imirimo yose uyu muntu akora. Uburenganzira bwa NYIR'IKIGO ntibuhabwa abakozi.
Reference:|Référence :|Icyerekana :
Served by:|Servi par :|Wakiriye :
Supply date:|Date de livraison :|Itariki yo gutanga ibicuruzwa :
Payment due:|Échéance du paiement :|Itariki ntarengwa yo kwishyura :
Currency:|Devise :|Ifaranga :
Received:|Reçu :|Ibyakiriwe :
Exchange rate:|Taux de change :|Igipimo cy'ivunjisha :
Payments|Paiements|Ubwishyu
Track movements|Suivre les mouvements|Kurikirana imigendekere y'ububiko
Delete|Supprimer|Siba
This makes the product inactive and removes it from new sales and purchases. Existing stock, invoices and history are kept. You can reactivate it using Edit.|Le produit sera désactivé et ne pourra plus être utilisé dans de nouvelles ventes ou achats. Le stock, les factures et l'historique restent conservés. Vous pouvez le réactiver avec Modifier.|Iki gicuruzwa kizahagarikwa ntikongere gukoreshwa mu kugura cyangwa kugurisha bishya. Ububiko, inyemezabuguzi n'amateka biragumaho. Ushobora kongera kugikoresha ukoresheje Hindura.
Confirm delete|Confirmer la suppression|Emeza gusiba
Enter your normal purchase and selling prices once. KBP converts quantities and prices automatically.|Saisissez une seule fois vos prix habituels d'achat et de vente. KBP convertit automatiquement les quantités et les prix.|Andika rimwe ibiciro bisanzwe byo kugura no kugurisha. KBP ihita ihindura ingano n'ibiciro.
1. Product information|1. Informations sur le produit|1. Amakuru y'igicuruzwa
Choose how this product is counted in stock. You can purchase it in larger packages.|Choisissez l'unité de suivi du produit en stock. Vous pouvez l'acheter en emballages plus grands.|Hitamo igipimo cyo kubariramo iki gicuruzwa mu bubiko. Ushobora kukigura mu mapaki manini.
Product name|Nom du produit|Izina ry'igicuruzwa
Smallest tracked unit|Plus petite unité suivie|Igipimo gito kibarirwamo
Stock is stored in this unit; it cannot change after creation.|Le stock est enregistré dans cette unité ; elle ne peut pas être modifiée après la création.|Ububiko bubarirwa muri iki gipimo; ntigihindurwa nyuma yo kurema igicuruzwa.
Custom stock unit|Unité de stock personnalisée|Igipimo cy'ububiko cyihariye
This existing product tracks stock in|Ce produit existant suit le stock en|Iki gicuruzwa gisanzwe kibarirwa mu
s. Do not enter the number of bottles/kg inside a package as the stock quantity. Preserve historical stock; create a new base-unit product after reconciliation if you need to sell individual contents.|. Ne saisissez pas le nombre de bouteilles ou de kilogrammes par emballage comme quantité en stock. Conservez l'historique ; créez un nouveau produit après rapprochement pour vendre les unités individuelles.|. Ntukandike amacupa cyangwa ibiro biri mu ipaki nk'umubare w'ububiko. Gumana amateka; niba ukeneye kugurisha ibiri imbere buri kimwe, banza uhuze imibare maze ureme igicuruzwa gishya.
2. How you purchase it|2. Comment vous l'achetez|2. Uko ugura iki gicuruzwa
Enter the amount paid for the actual purchase unit; no division required.|Saisissez le prix payé pour l'unité réellement achetée, sans division.|Andika amafaranga wishyuye ku gipimo waguze; nta kugabanya gukenewe.
Choose an existing package to edit|Choisir un emballage existant à modifier|Hitamo ipaki isanzwe yo guhindura
+ Use base unit / create new package|+ Utiliser l'unité de base / créer un emballage|+ Koresha igipimo cy'ibanze / rema ipaki nshya
Other / custom package|Autre / emballage personnalisé|Indi paki / ipaki yihariye
Custom package name|Nom de l'emballage personnalisé|Izina ry'ipaki yihariye
How many|Combien de|Ni bingahe
in one|dans un|muri imwe
Registered purchase cost per|Coût d'achat enregistré par|Igiciro cy'ubuguzi cyanditswe kuri
Automatic cost per|Coût automatique par|Igiciro kibarwa ku
(from the|(à partir du| (kivuye ku
purchase price). You do not need to calculate or enter this.|prix d'achat). Aucun calcul ni saisie supplémentaire n'est nécessaire.|giciro cyo kugura). Ntukeneye kubibara cyangwa kubyandika.
Additional unit name|Nom de l'unité supplémentaire|Izina ry'ikindi gipimo
Remove this package|Supprimer cet emballage|Kuraho iyi paki
+ Add another buying or selling unit|+ Ajouter une autre unité d'achat ou de vente|+ Ongeraho ikindi gipimo cyo kugura cyangwa kugurisha
3. How you sell it|3. Comment vous le vendez|3. Uko ugurisha iki gicuruzwa
Retail can be per individual stock unit, while wholesale can be per package.|La vente au détail peut se faire à l'unité de stock et la vente en gros par emballage.|Kugurisha ku giti bishobora gukorwa ku gipimo kimwe, naho kurangura bikaba ku ipaki.
Retail price for 1|Prix de détail pour 1|Igiciro cyo kugurisha kimwe
Wholesale price for 1|Prix de gros pour 1|Igiciro cyo kurangura kimwe
Retail price per|Prix de détail par|Igiciro cyo kugurisha kuri
(optional)|(facultatif)|(si ngombwa)
Minimum selling price per|Prix de vente minimum par|Igiciro gito cyo kugurisha kuri
Low-stock alert (in|Alerte de stock faible (en|Imenyesha ry'ububiko buke (muri
Discount and return rules (optional)|Règles de remise et de retour (facultatif)|Amategeko yo kugabanya ibiciro no gusubiza (si ngombwa)
Allow authorized discounts|Autoriser les remises approuvées|Emerera kugabanya ibiciro byemewe
Maximum cashier discount (%)|Remise maximale du caissier (%)|Igabanyirizwa ntarengwa ry'umukarani (%)
Customer return period (days)|Délai de retour client (jours)|Iminsi umukiriya yemerewe gusubizamo
Allow fractional stock units (e.g., 0.5 kg; not 0.5 phone)|Autoriser les fractions d'unités (ex. : 0,5 kg, mais pas 0,5 téléphone)|Emerera ibipimo bituzuye (urugero 0,5 kg; si 0,5 telefoni)
Manager discounts still require a reason; amounts below the minimum selling price are rejected. An item such as a phone should not allow fractional units.|Les remises du responsable exigent toujours une justification ; les prix inférieurs au minimum sont refusés. Un article comme un téléphone ne doit pas permettre de fractions.|Kugabanya igiciro byemejwe n'umuyobozi bigomba kugira impamvu; igiciro kiri munsi y'igito cyemewe nticyakirwa. Igicuruzwa nka telefoni ntikigomba kugabanywamo uduce.
Branch-specific price (optional)|Prix propre à la succursale (facultatif)|Igiciro cy'ishami (si ngombwa)
Selling price per|Prix de vente par|Igiciro cyo kugurisha kuri
One Save updates the product and its units together. Stock is added only through goods receiving.|Une seule sauvegarde met à jour le produit et ses unités. Le stock augmente uniquement lors de la réception des marchandises.|Kubika rimwe bivugurura igicuruzwa n'ibipimo byacyo. Ububiko bwiyongera gusa igihe ibicuruzwa byakiriwe.
Receive|Réceptionner|Akira
Ordered:|Commandé :|Ibyatumijwe :
· Accepted:|· Accepté :|· Ibyemewe :
· Claimed:|· Réclamé :|· Ibyasabiwe indishyi :
· Still to check:|· Reste à vérifier :|· Ibisigaye kugenzurwa :
All quantities have been received or recorded as supplier claims.|Toutes les quantités ont été reçues ou déclarées en réclamation auprès du fournisseur.|Ingano zose zarakiriwe cyangwa zanditswe nk'ibibazo bigomba gukemurwa n'uwatanze ibicuruzwa.
Confirm this delivery|Confirmer cette livraison|Emeza iri tungwa ry'ibicuruzwa
Add|Ajouter|Ongeraho
to sellable stock.|au stock disponible à la vente.|mu bubiko bushobora kugurishwa.
Damaged:|Endommagé :|Ibyangiritse :
· Missing:|· Manquant :|· Ibyabuze :
will remain to check. Confirm only after checking the actual goods.|resteront à vérifier. Confirmez uniquement après vérification physique.|bizasigara bigomba kugenzurwa. Emeza umaze kugenzura ibicuruzwa nyabyo.
Confirm received goods|Confirmer les marchandises reçues|Emeza ibicuruzwa byakiriwe
Back to quantities|Retour aux quantités|Subira ku ngano
Accepted quantity (|Quantité acceptée (|Ingano yemewe (
All remaining goods arrived in good condition|Toutes les marchandises restantes sont arrivées en bon état|Ibicuruzwa byose byari bisigaye byageze bimeze neza
Damaged or missing goods|Marchandises endommagées ou manquantes|Ibicuruzwa byangiritse cyangwa byabuze
Damaged quantity|Quantité endommagée|Ingano yangiritse
Missing quantity|Quantité manquante|Ingano yabuze
Next action|Action suivante|Igikorwa gikurikira
Hold / inspect|Mettre en attente / inspecter|Bika utegereje / genzura
Request replacement|Demander un remplacement|Saba ibindi byo gusimbuza
Request supplier credit|Demander un avoir fournisseur|Saba ideni ryishyurwa n'uwatanze ibicuruzwa
Delivery note / reason|Bon de livraison / motif|Inyandiko y'ibyazanywe / impamvu
Damaged and missing goods are recorded as claims. They do not increase stock or automatically issue a refund.|Les articles endommagés et manquants sont enregistrés comme réclamations. Ils n'augmentent pas le stock et ne déclenchent pas de remboursement automatique.|Ibyangiritse n'ibyabuze byandikwa nk'ibirego ku wabitanze. Ntibyongera ububiko kandi ntibisubiza amafaranga byikora.
The total exceeds the quantity still to check.|Le total dépasse la quantité restant à vérifier.|Igiteranyo kirenze ingano isigaye kugenzurwa.
Enter a reason for damaged or missing goods.|Indiquez le motif des marchandises endommagées ou manquantes.|Andika impamvu y'ibyangiritse cyangwa ibyabuze.
Review received goods|Vérifier les marchandises reçues|Suzuma ibicuruzwa byakiriwe
Review the details before confirming.|Vérifiez les détails avant de confirmer.|Banza usuzume amakuru mbere yo kwemeza.
Adjust or transfer stock|Ajuster ou transférer le stock|Kosora cyangwa wimure ububiko
· Stock movements|· Mouvements de stock|· Imigendekere y'ububiko
Latest 500 movements for this product at the selected stock location. Positive quantities add stock; negative quantities remove it.|Les 500 derniers mouvements de ce produit à l'emplacement sélectionné. Les quantités positives augmentent le stock ; les négatives le diminuent.|Imigendekere 500 iheruka y'iki gicuruzwa mu bubiko bwatoranyijwe. Ingano nziza yongera ububiko; ingano mbi irabugabanya.
Commerce setup required|Configuration commerciale requise|Igenamiterere ry'ubucuruzi rirakenewe
Purchasing and selling need Inventory, Retail and Accounting modules. These are core Commerce operations.|Les achats et les ventes nécessitent les modules Stocks, Vente au détail et Comptabilité. Ce sont des fonctions essentielles du Commerce.|Kugura no kugurisha bisaba ibice by'Ububiko, Kugurisha ku giti n'Ibaruramari. Ni ibikorwa by'ingenzi by'Ubucuruzi.
Enable purchasing and sales|Activer les achats et les ventes|Fungura kugura no kugurisha
Ask the business owner to enable Commerce operations.|Demandez au propriétaire d'activer les opérations commerciales.|Saba nyir'ikigo gufungura ibikorwa by'ubucuruzi.
Before continuing:|Avant de continuer :|Mbere yo gukomeza :
choose the branch and the stock location in the selectors at the top of this page. You can then review and post your|choisissez la succursale et l'emplacement du stock en haut de cette page. Vous pourrez ensuite vérifier et enregistrer vos|hitamo ishami n'ububiko mu mahitamo ari hejuru y'uru rupapuro. Hanyuma ushobora gusuzuma no kwandika
Choose products and quantities|Choisir les produits et les quantités|Hitamo ibicuruzwa n'ingano
Review total and payment|Vérifier le total et le paiement|Suzuma igiteranyo n'ubwishyu
lines)|lignes)|imirongo)
No active products. Create a product in Products first.|Aucun produit actif. Créez d'abord un produit dans Produits.|Nta bicuruzwa bikora. Banza ureme igicuruzwa ahitwa Ibicuruzwa.
Qty|Qté|Ingano
Catalog price (|Prix du catalogue (|Igiciro cyo ku rutonde (
Line estimate|Estimation de la ligne|Ikigereranyo cy'uyu murongo
Available:|Disponible :|Ibihari :
Override:|Remplacement :|Igiciro gihinduwe :
Before discounts / tax|Avant remises / taxes|Mbere yo kugabanya ibiciro / umusoro
Add a product above to begin.|Ajoutez un produit ci-dessus pour commencer.|Ongeraho igicuruzwa hejuru kugira ngo utangire.
Fixed amount (|Montant fixe (|Amafaranga adahinduka (
Percentage (%)|Pourcentage (%)|Ijanisha (%)
Only one item expands at a time. The product list scrolls independently, including purchases with 20 or more lines.|Un seul article peut être développé à la fois. La liste des produits défile indépendamment, même pour les achats de 20 lignes ou plus.|Igicuruzwa kimwe gusa kiramburwa icyarimwe. Urutonde rw'ibicuruzwa rushobora kuzamurwa no kumanurwa ukwabwo, no ku byaguzwe bifite imirongo 20 cyangwa irenga.
Product lines|Lignes de produits|Imirongo y'ibicuruzwa
Quantity total (mixed units)|Quantité totale (unités mixtes)|Igiteranyo cy'ingano (ibipimo bitandukanye)
By item|Par article|Kuri buri gicuruzwa
Final discounts, taxes and exchange rates are calculated by the backend when you review.|Les remises finales, taxes et taux de change sont calculés par le serveur lors de la vérification.|Igabanyirizwa ry'ibiciro rya nyuma, imisoro n'ibipimo by'ivunjisha bibarwa na seriveri igihe usuzuma.
Reviewed total|Total vérifié|Igiteranyo cyasuzumwe
Confirmation is uncertain. Retry the same request to avoid duplicates.|Confirmation incertaine. Réessayez la même demande pour éviter les doublons.|Kwemeza ntibizwi neza. Ongera wohereze icyifuzo kimwe kugira ngo wirinde ibyanditswe kabiri.
Retry same transaction|Réessayer la même transaction|Ongera ugerageze igikorwa kimwe
Supply date|Date de livraison|Itariki yo gutanga ibicuruzwa
Payment and checkout|Paiement et encaissement|Kwishyura no kurangiza igurisha
Cash|Espèces|Amafaranga y'intoki
Mobile Money|Paiement mobile|Amafaranga kuri telefoni
Bank transfer|Virement bancaire|Kohereza amafaranga kuri banki
Card|Carte bancaire|Ikarita ya banki
Approved customer credit|Crédit client approuvé|Ideni ry'umukiriya ryemewe
Amount to collect|Montant à encaisser|Amafaranga yo kwakira
Automatically selected from reviewed cart; taxes and discounts included.|Sélectionné automatiquement d'après le panier vérifié, taxes et remises incluses.|Bihita bitoranywa hashingiwe ku byasuzumwe, harimo imisoro n'igabanyirizwa ry'ibiciro.
You have no open cashier shift. Go to|Aucune caisse ouverte. Allez à|Nta shifuti y'umubitsi ifunguye. Jya kuri
to open one before accepting cash.|pour en ouvrir une avant d'accepter des espèces.|kugira ngo uyifungure mbere yo kwakira amafaranga y'intoki.
No active matching|Aucun compte actif correspondant|Nta konti ikora ihuye na
account for|pour|kuri
. The owner can create one in Payment accounts.|. Le propriétaire peut en créer un dans Comptes de paiement.|. Nyir'ikigo ashobora kuyirema muri Konti zo kwishyuriraho.
The customer must have an approved credit limit. A credit sale leaves an amount receivable.|Le client doit disposer d'une limite de crédit approuvée. Une vente à crédit crée une créance.|Umukiriya agomba kuba afite umupaka w'ideni wemewe. Kugurisha ku ideni bituma habaho amafaranga yishyuzwa.
Invoice cost and supplier discount are calculated above. Receive goods separately after posting. Payment can be recorded later or added below.|Le montant facturé et la remise fournisseur sont calculés ci-dessus. Réceptionnez les marchandises séparément après l'enregistrement. Le paiement peut être ajouté ci-dessous ou ultérieurement.|Igiciro cy'inyemezabuguzi n'igabanyirizwa ry'uwatanze ibicuruzwa bibarwa hejuru. Akira ibicuruzwa ukwabyo nyuma yo kwandika. Ubwishyu bushobora kwandikwa nyuma cyangwa munsi aha.
Split payments / advanced payment entry|Paiements fractionnés / saisie avancée|Kwishyura mu byiciro / kwandika ubwishyu mu buryo burambuye
· Edit|· Modifier|· Hindura
No payment accounts yet. Click Create payment account to add one.|Aucun compte de paiement. Cliquez sur Créer un compte de paiement pour en ajouter un.|Nta konti zo kwishyuriraho zirahari. Kanda Rema konti yo kwishyuriraho kugira ngo wongere imwe.
A4 / Save PDF|A4 / Enregistrer en PDF|A4 / Bika nka PDF
Payments and refunds|Paiements et remboursements|Ubwishyu n'amafaranga asubizwa
Record a return|Enregistrer un retour|Andika ibyagaruwe
Supplier claim:|Réclamation fournisseur :|Ikirego ku watanze ibicuruzwa :
· Damaged|· Endommagé|· Ibyangiritse
· Missing|· Manquant|· Ibyabuze
The owner must enable Accounting and approve opening inventory valuation before shifts can open.|Le propriétaire doit activer la comptabilité et approuver la valorisation initiale des stocks avant l'ouverture des caisses.|Nyir'ikigo agomba gufungura Ibaruramari no kwemeza agaciro k'ububiko bw'ibanze mbere yo gufungura shifuti.
Select a branch before opening a shift.|Sélectionnez une succursale avant d'ouvrir une caisse.|Hitamo ishami mbere yo gufungura shifuti.
No active cash account exists for this business. Opening a cashier shift requires a cash payment account.|Aucun compte espèces actif pour cette entreprise. L'ouverture d'une caisse exige un compte de paiement en espèces.|Nta konti y'amafaranga y'intoki ikora muri iki kigo. Gufungura shifuti y'umubitsi bisaba konti y'amafaranga y'intoki.
Create cash account|Créer un compte espèces|Rema konti y'amafaranga y'intoki
Ask the owner to create a cash account under Payment accounts.|Demandez au propriétaire de créer un compte espèces sous Comptes de paiement.|Saba nyir'ikigo kurema konti y'amafaranga y'intoki muri Konti zo kwishyuriraho.
You already have an open shift on|Vous avez déjà une caisse ouverte le|Usanzwe ufite shifuti ifunguye ku
. Close it before opening another.|. Fermez-la avant d'en ouvrir une autre.|. Yifunge mbere yo gufungura indi.
Payment account balances|Soldes des comptes de paiement|Amafaranga asigaye kuri konti zo kwishyuriraho
Cumulative recorded balance through|Solde cumulé enregistré jusqu'au|Amafaranga yose yanditswe kugeza ku
for the selected branch scope. Includes receipts, supplier payments, refunds and expenses. Unrecorded opening funds and cash drawer floats are excluded. Each account is shown in its own currency.|pour la portée des succursales sélectionnées. Comprend les recettes, paiements fournisseurs, remboursements et dépenses. Les fonds d'ouverture et fonds de caisse non enregistrés sont exclus. Chaque compte est affiché dans sa devise.|ku mashami yatoranyijwe. Harimo amafaranga yakiriwe, ayishyuwe abatanga ibicuruzwa, ayasubijwe n'ibyakoreshejwe. Amafaranga yo gutangira atanditswe ntabarwamo. Buri konti igaragazwa mu ifaranga ryayo.
Inactive account|Compte inactif|Konti idakora
No payment accounts found.|Aucun compte de paiement trouvé.|Nta konti yo kwishyuriraho yabonetse.
Save base currency|Enregistrer la devise de base|Bika ifaranga shingiro
Base currency is locked because business records already exist.|La devise de base est verrouillée car des données commerciales existent déjà.|Ifaranga shingiro ntirishobora guhindurwa kuko amakuru y'ubucuruzi yamaze kwandikwa.
Choose your accounting and default transaction currency before adding products, exchange rates or financial records.|Choisissez votre devise comptable et par défaut avant d'ajouter des produits, des taux de change ou des données financières.|Hitamo ifaranga shingiro mbere yo kwandika ibicuruzwa, ibipimo by'ivunjisha cyangwa amakuru y'imari.
`;
const currencyRows=`
Reservation actions|Actions de réservation|Ibikorwa byo kubika icyumba
Back to reservations|Retour aux réservations|Subira ku byumba byabitswe
Stay dates|Dates du séjour|Amatariki yo gucumbika
Check availability before choosing rooms.|Vérifiez les disponibilités avant de choisir les chambres.|Reba ibyumba bihari mbere yo guhitamo.
Customer details|Informations du client|Amakuru y'umukiriya
Choose rooms|Choisir les chambres|Hitamo ibyumba
Selected rooms|Chambres sélectionnées|Ibyumba byatoranyijwe
Available rooms will appear after checking your dates.|Les chambres disponibles apparaîtront après vérification des dates.|Ibyumba bihari biragaragara nyuma yo kugenzura amatariki.
Reservation summary|Résumé de la réservation|Incamake y'icyumba kibitswe

Configured rate|Tarif configuré|Igiciro cyashyizweho
Hotel currency|Devise de l'hôtel|Ifaranga rya hoteli
night|nuit|ijoro
Custom room rate (optional)|Tarif personnalisé de la chambre (facultatif)|Igiciro cyihariye cy'icyumba (si itegeko)
Leave empty to use the configured rate.|Laissez vide pour utiliser le tarif configuré.|Hasige ubusa kugira ngo ukoreshe igiciro cyashyizweho.
Enter a nonnegative amount with up to 15 digits and 4 decimal places.|Saisissez un montant positif ou nul avec au maximum 15 chiffres entiers et 4 décimales.|Andika amafaranga atari munsi ya zeru, afite imibare itarenze 15 n'imibare 4 nyuma y'akitso.
Effective nightly rate|Tarif par nuit appliqué|Igiciro cy'ijoro gikoreshwa

Edit payment account|Modifier le compte de paiement|Hindura konti yo kwishyuriraho
Create payment account|Créer un compte de paiement|Kora konti yo kwishyuriraho
Payment method and currency are locked after account activity. Create another account to use a different method or currency.|Le mode de paiement et la devise sont verrouillés après utilisation du compte. Créez un autre compte pour un mode ou une devise différent.|Uburyo bwo kwishyura n'ifaranga ntibihinduka iyo konti yakoreshejwe. Kora indi konti kugira ngo ukoreshe ubundi buryo cyangwa irindi faranga.

Reactivate|Réactiver|Ongera ukore
Your open cash shift uses another currency. Use advanced payment entry with the amount in the account currency, or open a matching cash shift.|Votre session de caisse utilise une autre devise. Utilisez le paiement avancé avec le montant dans la devise du compte, ou ouvrez une session correspondante.|Kashi yafunguwe ikoresha irindi faranga. Koresha uburyo burambuye bwo kwishyura wandike amafaranga mu ifaranga rya konti, cyangwa ufungure kashi ihuje ifaranga.
Uncheck Active to deactivate. Payment history is retained. For a different currency or method, create another account.|Décochez Actif pour désactiver. L'historique est conservé. Pour une autre devise ou méthode, créez un autre compte.|Kuraho Akora kugira ngo uhagarike konti. Amateka yo kwishyura aragumaho. Ku rindi faranga cyangwa ubundi buryo, kora indi konti.

Accounting base currency|Devise comptable de base|Ifaranga shingiro ry’ibaruramari
Default transaction currency|Devise par défaut des transactions|Ifaranga risanzwe rikoreshwa mu bucuruzi
New transactions and editable catalog prices use this currency. Past records and the accounting base currency stay unchanged.|Les nouvelles transactions et les prix modifiables utilisent cette devise. Les anciennes opérations et la devise comptable restent inchangées.|Ibikorwa bishya n'ibiciro bishobora guhindurwa bikoresha iri faranga. Amakuru ya kera n'ifaranga shingiro ntibihinduka.
Preview converted prices|Aperçu des prix convertis|Reba ibiciro byavunjwe
Review the conversion, then edit individual product prices if needed.|Vérifiez la conversion, puis ajustez les prix des produits si nécessaire.|Suzuma ivunjisha, hanyuma uhindure ibiciro by'ibicuruzwa niba bikenewe.
Confirm currency change|Confirmer le changement de devise|Emeza ihindurwa ry'ifaranga
Enable the currency and add its current exchange rate first. Refresh if the rate or default changed.|Activez la devise et ajoutez son taux de change actuel. Actualisez si le taux ou la devise par défaut a changé.|Banza wemeze ifaranga kandi ushyireho igipimo cy'ivunjisha. Ongera ufungure niba igipimo cyangwa ifaranga byahindutse.
`;
export const messages:Record<Locale,Record<string,string>>={en:{},fr:{},rw:{}};
for(const line of (rows+currencyRows).trim().split("\n").filter(line=>line.trim())){const [en,fr,rw]=line.split("|");messages.en[en]=en;messages.fr[en]=fr;messages.rw[en]=rw;}
for(const [old,key] of Object.entries({reservations:"Reservations",availability:"Check availability",adults:"Adults",children:"Children",preview:"Preview order",confirm:"Approve"}))for(const locale of locales)messages[locale][old]=messages[locale][key];








