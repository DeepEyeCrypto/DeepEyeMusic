import os
import glob
import re

files = glob.glob('/Users/enayat/Documents/DeepEyeMusicPro/app/src/main/java/**/*.kt', recursive=True)
for file in files:
    with open(file, 'r', encoding='utf-8') as f:
        content = f.read()
    
    if '.collectAsState()' in content:
        print(f"Patching {file}")
        content = content.replace('.collectAsState()', '.collectAsStateWithLifecycle()')
        
        import_str = 'import androidx.lifecycle.compose.collectAsStateWithLifecycle\n'
        if import_str not in content:
            content = re.sub(r'^(import .*)', import_str + r'\1', content, count=1, flags=re.MULTILINE)
        
        with open(file, 'w', encoding='utf-8') as f:
            f.write(content)
