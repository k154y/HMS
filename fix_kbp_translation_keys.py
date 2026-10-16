#!/usr/bin/env python3
"""Add verified missing KBP translations, preserving existing repository edits."""
from pathlib import Path
import sys

ROOT = Path.cwd()
FILE = ROOT / 'apps/web/lib/i18n.ts'
ENTRIES = [
    ('Roles', 'Rôles', 'Inshingano'),
    ('Staff roles (select more than one)', 'Rôles du personnel (plusieurs choix possibles)', 'Inshingano z’abakozi (ushobora guhitamo zirenze imwe)'),
    ('Create product', 'Créer un produit', 'Ongeraho igicuruzwa'),
    ('Stock operations', 'Opérations de stock', 'Ibikorwa by’ububiko'),
    ('Select registered product', 'Sélectionner un produit enregistré', 'Hitamo igicuruzwa cyanditswe'),
    ('Add product from catalog', 'Ajouter un produit du catalogue', 'Ongeraho igicuruzwa kiri ku rutonde'),
    ('Print receipt', 'Imprimer le reçu', 'Sohora inyemezabwishyu'),
    ('Print invoice', 'Imprimer la facture', 'Sohora inyemezabuguzi'),
]
if not FILE.is_file():
    sys.exit('STOP: Run this script from the HMS repository root.')
raw = FILE.read_bytes()
newline = b'\r\n' if b'\r\n' in raw else b'\n'
text = raw.decode('utf-8')
normalized = text.replace('\r\n', '\n')
marker = '\n`;\nexport const messages:'
if normalized.count(marker) != 1:
    sys.exit('STOP: Translation table structure differs from reviewed version; file unchanged.')
if 'for(const line of rows.trim().split("\\n")' not in normalized:
    sys.exit('STOP: Translation parser differs from reviewed version; file unchanged.')
existing = {line.split('|',1)[0] for line in normalized.split('const rows=`',1)[1].split('\n`;\nexport const messages:',1)[0].splitlines() if '|' in line}
additional = [row for row in ENTRIES if row[0] not in existing]
if not additional:
    print('All eight verified translation keys are already present. No changes.')
    sys.exit(0)
for row in additional:
    if any('|' in field or '\n' in field for field in row):
        sys.exit('STOP: Invalid translation entry')
block = ''.join('|'.join(row)+'\n' for row in additional)
updated = normalized.replace(marker, '\n'+block+'`;\nexport const messages:', 1)
FILE.write_bytes(updated.replace('\n', '\r\n' if newline == b'\r\n' else '\n').encode('utf-8'))
print(f'Added {len(additional)} verified keys to {FILE.relative_to(ROOT)}:')
for key, _, _ in additional:
    print('  + '+key)
print('No component, backend, Git branch, or existing translations were altered.')
