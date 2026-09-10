"""Shared database settings; passwords are never interpolated into URLs."""
import os
from pathlib import Path
from dotenv import load_dotenv
from sqlalchemy import URL, create_engine

ROOT = Path(__file__).resolve().parent

def database_engine():
    load_dotenv(ROOT / '.env')
    required = ('DB_USER', 'DB_PASSWORD', 'DB_NAME')
    missing = [name for name in required if not os.getenv(name)]
    if missing:
        raise ValueError('Missing settings: ' + ', '.join(missing))
    url = URL.create('mysql+pymysql', username=os.environ['DB_USER'],
                     password=os.environ['DB_PASSWORD'], host=os.getenv('DB_HOST', 'localhost'),
                     port=int(os.getenv('DB_PORT', '3306')), database=os.environ['DB_NAME'])
    return create_engine(url, pool_pre_ping=True)
