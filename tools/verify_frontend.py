"""Validate the frontend source boundary, packaged assets, and JavaScript syntax."""
from pathlib import Path
from html.parser import HTMLParser
import json,re,subprocess,sys
ROOT=Path(__file__).resolve().parents[1]
FRONTEND=ROOT/'src/main/frontend'

class Markup(HTMLParser):
    def __init__(self):
        super().__init__();self.fields=[];self.actions=[];self.assets=[];self.inline=[];self.duplicates=[]
    def handle_starttag(self,tag,attrs):
        keys=[k for k,v in attrs]
        self.duplicates.extend(k for k in set(keys) if keys.count(k)>1)
        attrs=dict(attrs)
        if tag in ('input','select','textarea'):
            binding=attrs.get('th:field') or attrs.get('name')
            if binding:self.fields.append(binding)
        if tag=='form':self.actions.append(attrs.get('th:action') or attrs.get('action') or '')
        for key,value in attrs.items():
            if key.startswith('on') or key=='style':self.inline.append(key)
            if key in ('th:href','th:src') and value and re.fullmatch(r'@\{/[^{}]+\}',value):
                asset=value[2:-1].split('?')[0]
                if asset.startswith(('/css/','/js/','/fonts/','/images/')):self.assets.append(asset)

def verify():
    errors=[];warnings=[]
    manifest=json.loads((ROOT/'tools/frontend-screen-map.json').read_text(encoding='utf-8'))
    for item in manifest:
        folder=FRONTEND/'screens'/item['screen']
        for name in ('index.html','style.css','script.js'):
            if not (folder/name).is_file():errors.append(f'Missing {folder/name}')
        page=folder/'index.html'
        if not page.exists():continue
        html=page.read_text(encoding='utf-8-sig');parser=Markup();parser.feed(html)
        if parser.duplicates:errors.append(f'{item["screen"]}: duplicate attributes {parser.duplicates}')
        if parser.inline:errors.append(f'{item["screen"]}: inline presentation/handlers: {parser.inline}')
        if re.search(r'<script\b(?![^>]*\bsrc\s*=)(?![^>]*type=[\"\x27]application/json)',html,re.I):errors.append(f'{item["screen"]}: executable embedded script')
        if re.search(r'fonts\.googleapis|/css/(bootstrap|drivenow|app)\.css',html):errors.append(f'{item["screen"]}: legacy/remote stylesheet')
        for asset in parser.assets:
            if not (ROOT/'target/classes/static'/asset.lstrip('/')).exists():errors.append(f'Unpackaged {asset} in {item["screen"]}')
        original=ROOT/item['source']
        if original.exists():
            old=Markup();old.feed(original.read_text(encoding='utf-8-sig'))
            for field in set(old.fields)-set(parser.fields):warnings.append(f'{item["screen"]}: removed/renamed form field {field}')
            for action in set(old.actions)-set(parser.actions):errors.append(f'{item["screen"]}: removed form action {action}')
        if not (ROOT/'target/classes/templates'/(item['view']+'.html')).exists():errors.append(f'Unpackaged template {item["view"]}')
    js_files=list(FRONTEND.rglob('*.js'))
    for path in js_files:
        script=path.read_text(encoding='utf-8-sig')
        if re.search(r'\[\[\$\{|\[\(\$\{',script):errors.append(f'Unprocessed Thymeleaf in {path.relative_to(ROOT)}')
        result=subprocess.run(['node','--check',str(path)],capture_output=True,text=True)
        if result.returncode:errors.append(f'{path.relative_to(ROOT)}: {result.stderr.strip()}')
    for path in (FRONTEND/'shared/fragments').glob('*.html'):
        html=path.read_text(encoding='utf-8-sig');parser=Markup();parser.feed(html)
        if parser.duplicates:errors.append(f'{path.name}: duplicate attributes {parser.duplicates}')
        for asset in parser.assets:
            if not (ROOT/'target/classes/static'/asset.lstrip('/')).exists():errors.append(f'Unpackaged shared asset {asset}')
    print(f'{len(manifest)} screens; {len(js_files)} JavaScript files checked.')
    for line in warnings:print('REVIEW:',line)
    for line in errors:print('ERROR:',line)
    print(f'{len(errors)} errors; {len(warnings)} form changes to review.')
    return bool(errors)

if __name__=='__main__':sys.exit(verify())
