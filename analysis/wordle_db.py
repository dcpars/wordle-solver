from db_client import DbClient
import psycopg2

class WordleDb:
    SELECT_WORD_COUNTS = """
        SELECT id, word, count 
        FROM wordle_solver.t_word_counts 
        WHERE word in {};
        """

    UPSERT_WORD_COUNTS = """
        INSERT INTO wordle_solver.t_word_counts(word, count)
        VALUES {}
        ON CONFLICT(word)
        DO UPDATE SET count = wordle_solver.t_word_counts.count + excluded.count
        RETURNING id, word, count;
        """

    SELECT_URL = """
        SELECT id, url, word_count
        FROM wordle_solver.t_scraped_urls
        WHERE url = '{}';
        """

    INSERT_URL = """
        INSERT INTO wordle_solver.t_scraped_urls(url, word_count)
        VALUES ('{}', {})
        RETURNING id, url, word_count;
        """

    # This means each instance of WordleDb - which should ideally    be one -
    # holds an instance of the database client. Consider moving the
    # database client to a static variable.
    def __init__(self):
        self.db_client = DbClient("psql", 5432)  # This will need to be configured per environment.

    # Given a dictionary of words and their respective counts, store
    # in the database. If a word already exists in the database,
    # increment the count accordingly. Returns an array of dictionaries
    # containing the updated word counts for the words provided.
    def store_word_counts(self, word_counts):
        if word_counts is not None and len(word_counts) > 0:
            query = ""
            try:
                query = self.__build_upsert_query(word_counts)
                return self.db_client.insert(query)
            except:
                print("Error storing word counts. Attempted query: {}".format(query))
        return []

    def __build_upsert_query(self, word_counts):
        formatted_word_counts = []
        for word in word_counts:
            formatted = "('{}',{})".format(word, word_counts[word])
            formatted_word_counts.append(formatted)
        joined_word_counts = ','.join(formatted_word_counts)
        return self.UPSERT_WORD_COUNTS.format(joined_word_counts)

    def url_hasnt_been_scraped(self, url):
        query = self.SELECT_URL.format(url)
        result = self.db_client.select(query)
        return result is None or len(result) == 0

    def store_url(self, url, word_count):
        query = self.INSERT_URL.format(url, word_count)
        return self.db_client.insert(query)
