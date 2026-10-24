#!/usr/bin/env python3
"""Fail CI if source accidentally contains an actual credential or signing key."""
from pathlib import Path
import re, sys
root = Path(__file__).resolve().parents[1]
patterns = [re.compile(r'gsk_[A-Za-z0-9]{30,}'), re.compile(r'-----BEGIN (?:RSA |EC )?PRIVATE KEY-----'), re.compile(r'gh[pousr]_[A-Za-z0-9]{30,}')]
ignored = {'.git', '.gradle', 'build', 'node_modules'}
bad=[]
for path in root.rglob('*'):
    if not path.is_file() or ignored.intersection(path.relative_to(root).parts): continue
    if path.name in ('google-services.json','local.properties') or path.name.startswith('.env'): continue
    if path.stat().st_size > 2_000_000: continue
    try: text=path.read_text()
    except UnicodeError: continue
    if any(pattern.search(text) for pattern in patterns): bad.append(str(path.relative_to(root)))
if bad:
    print('Credential detected in:', ', '.join(bad)); sys.exit(1)
print('Source credential scan: passed')
