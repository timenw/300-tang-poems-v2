import sqlite3
c = sqlite3.connect("poems.db")
count = c.execute("SELECT COUNT(*) FROM poems").fetchone()[0]
print(f"Poems in DB: {count}")
# Check table names
tables = c.execute("SELECT name FROM sqlite_master WHERE type='table'").fetchall()
print(f"Tables: {tables}")
c.close()
