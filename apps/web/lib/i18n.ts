export const locales=["en","fr","rw"] as const;
export type Locale=(typeof locales)[number];
const rows=`
HotelPro — Hotel Management System|HotelPro — Système de gestion hôtelière|HotelPro — Sisitemu yo gucunga hoteli
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
`;
export const messages:Record<Locale,Record<string,string>>={en:{},fr:{},rw:{}};
for(const line of rows.trim().split("\n")){const [en,fr,rw]=line.split("|");messages.en[en]=en;messages.fr[en]=fr;messages.rw[en]=rw;}
for(const [old,key] of Object.entries({reservations:"Reservations",availability:"Check availability",adults:"Adults",children:"Children",preview:"Preview order",confirm:"Approve"}))for(const locale of locales)messages[locale][old]=messages[locale][key];








