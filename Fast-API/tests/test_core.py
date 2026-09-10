import importlib.util
import sys
import tempfile
import unittest
from pathlib import Path
from unittest.mock import Mock

ROOT = Path(__file__).resolve().parents[1]
sys.path.insert(0, str(ROOT))
from data_utils import parse_notes
from import_csv import read_records
from vector_core import SearchEngine, SearchBusyError
spec = importlib.util.spec_from_file_location('import_notes', ROOT.parent / 'recommendation' / 'import_notes.py')
notes_import = importlib.util.module_from_spec(spec)
spec.loader.exec_module(notes_import)

class CoreTests(unittest.TestCase):
    def test_actual_csv_is_valid_and_repeatable(self):
        first = read_records(ROOT / 'per_data.csv')
        self.assertEqual(first, read_records(ROOT / 'per_data.csv'))
        self.assertGreater(len(first), 4000)
        self.assertEqual(len(first), len({(r['brand'].casefold(), r['name'].casefold()) for r in first}))

    def test_original_pyramid_and_missing_groups_are_preserved(self):
        self.assertEqual(parse_notes('{"top":null,"middle":["Rose"],"base":["Musk"]}'),
                         {'top': [], 'middle': ['Rose'], 'base': ['Musk']})
        self.assertEqual(parse_notes('null'), [])
        self.assertEqual(parse_notes('["Rose","Rose"]'), ['Rose'])

    def test_translation_csv_is_valid(self):
        self.assertGreater(len(notes_import.read_notes(ROOT.parent / 'Note.csv')), 1000)

    def test_invalid_translation_is_rejected_before_database_work(self):
        with tempfile.TemporaryDirectory() as folder:
            path = Path(folder) / 'bad.csv'
            path.write_text('Note,note_kor\nRose,\n')
            with self.assertRaises(ValueError): notes_import.read_notes(path)

    def test_search_clamps_k_and_ignores_invalid_indices(self):
        model = Mock()
        index = Mock(ntotal=2)
        index.search.return_value = ([], [[0, -1, 1, 99]])
        engine = SearchEngine(model, index, [10, 20])
        self.assertEqual(engine.search('rose', 100), [{'id':10}, {'id':20}])
        self.assertEqual(index.search.call_args.args[1], 2)

    def test_invalid_search_is_rejected_before_inference(self):
        model = Mock(); index = Mock(ntotal=1)
        engine = SearchEngine(model, index, [1])
        for query, count in [('',20), ('x',0), ('x',101), ('x'*1001,20)]:
            with self.assertRaises(ValueError): engine.search(query,count)
        model.encode.assert_not_called()

    def test_busy_engine_does_not_queue_unbounded_work(self):
        engine = SearchEngine(Mock(), Mock(ntotal=1), [1])
        engine.gate.acquire(); engine.gate.acquire()
        with self.assertRaises(SearchBusyError): engine.search('rose',20)

    def test_inconsistent_index_fails_at_startup(self):
        with self.assertRaises(ValueError): SearchEngine(Mock(), Mock(ntotal=2), [1])

if __name__ == '__main__': unittest.main()
