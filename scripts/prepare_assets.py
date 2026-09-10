"""Create small, verified note thumbnails for both Vite and Spring Boot."""
import hashlib
import io
import json
import shutil
from pathlib import Path
from PIL import Image, ImageOps, UnidentifiedImageError

ROOT = Path(__file__).resolve().parents[1]
SOURCE = ROOT / 'perfume-frontend' / 'public'
FRONT = ROOT / 'perfume-frontend' / '.generated-public'
BACK = ROOT / 'recommendation' / 'build' / 'generated-assets'
DEFAULT = '<svg xmlns="http://www.w3.org/2000/svg" width="160" height="160" viewBox="0 0 160 160"><rect width="160" height="160" rx="16" fill="#f4eee7"/><path d="M80 120V50M80 90Q25 85 45 45Q85 50 80 90M80 75Q125 80 120 35Q80 40 80 75" fill="#a8b49b" stroke="#738465" stroke-width="3"/></svg>'

def property_escape(value):
    return value.replace('\\', '\\\\').replace(' ', '\\ ').replace('=', '\\=').replace(':', '\\:').replace('#', '\\#').replace('!', '\\!')

def main():
    manifest = {}
    for target in (FRONT / 'note-images', BACK / 'static' / 'note-images'):
        target.mkdir(parents=True, exist_ok=True)
        (target / 'default.svg').write_text(DEFAULT, encoding='utf-8')
    for path in sorted((SOURCE / 'note-images').glob('*')):
        if path.suffix.lower() not in {'.jpg', '.jpeg', '.png', '.webp'} or not path.stat().st_size:
            continue
        digest = hashlib.sha256(path.read_bytes()).hexdigest()[:24]
        name = digest + '.webp'
        front = FRONT / 'note-images' / name
        back = BACK / 'static' / 'note-images' / name
        if not front.exists():
            try:
                with Image.open(path) as original:
                    image = ImageOps.exif_transpose(original).convert('RGB')
                    image.thumbnail((320, 320))
                    output = io.BytesIO()
                    image.save(output, 'WEBP', quality=78, method=4)
                    front.write_bytes(output.getvalue())
            except (UnidentifiedImageError, OSError, Image.DecompressionBombError):
                continue
        if not back.exists():
            shutil.copyfile(front, back)
        key = path.stem.strip().lower()
        manifest.setdefault(key, '/note-images/' + name)
    # Explicit filenames win over bracket-free aliases.
    import re
    for key, value in list(manifest.items()):
        alias = re.sub(r'\s*\(.*?\)\s*', '', key).strip()
        if alias:
            manifest.setdefault(alias, value)
    for name in ('hero-perfume.jpeg', 'favicon.svg', 'icons.svg'):
        path = SOURCE / name
        if path.exists():
            FRONT.mkdir(parents=True, exist_ok=True)
            shutil.copyfile(path, FRONT / name)
    (BACK / 'note-images.properties').write_text('\n'.join(property_escape(k) + '=' + v for k, v in sorted(manifest.items())) + '\n', encoding='utf-8')
    (FRONT / 'note-images' / 'manifest.json').write_text(json.dumps(manifest, ensure_ascii=False), encoding='utf-8')
    print(f'Prepared {len(manifest)} note image mappings; missing/invalid images use the default.')

if __name__ == '__main__':
    main()
