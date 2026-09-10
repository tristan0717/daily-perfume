"""Normalize the actual CSV formats: comma text, JSON arrays and note pyramids."""
import json

def parse_notes(value):
    value = (value or '').strip()
    if not value or value.lower() in ('null', 'nan'):
        return []
    if value.startswith(('[', '{')):
        parsed = json.loads(value)
        if isinstance(parsed, dict):
            if not set(parsed).issubset({'top', 'middle', 'base'}):
                raise ValueError('Unknown note group')
            parsed = {k: ([] if items is None else items) for k, items in parsed.items()}
            for items in parsed.values():
                if not isinstance(items, list) or not all(isinstance(n, str) for n in items):
                    raise ValueError('Notes must be string arrays')
            return {k: list(dict.fromkeys(n.strip() for n in items if n.strip())) for k, items in parsed.items()}
        if not isinstance(parsed, list) or not all(isinstance(n, str) for n in parsed):
            raise ValueError('Notes must be string arrays')
        return list(dict.fromkeys(n.strip() for n in parsed if n.strip()))
    return list(dict.fromkeys(n.strip() for n in value.split(',') if n.strip()))

def note_text(value):
    notes = parse_notes(value)
    return ', '.join(n for values in notes.values() for n in values) if isinstance(notes, dict) else ', '.join(notes)
