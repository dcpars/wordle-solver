import psycopg2


# Use this client to perform operations on the database.
class DbClient:

    def __init__(self, host, port):
        self.host = host
        self.port = port

    def select(self, query):
        return self.__execute_query(query, False)

    def insert(self, query):
        return self.__execute_query(query, True)

    # Execute the query, returning an array of Dictionary objects
    # keyed by the column name.
    def __execute_query(self, query, autocommit=False):
        connection = psycopg2.connect(database="", user="", password="", host=self.host, port=self.port)
        connection.autocommit = autocommit
        cursor = connection.cursor()
        cursor.execute(query)
        result = cursor.fetchall()
        column_names = [desc[0] for desc in cursor.description]
        connection.close()
        return self.__transform_to_dict(column_names, result)

    # Transform the query result into an array of dictionaries,
    # keyed off the column name. The result is in the form or
    # an array of tuples, whose size corresponds to the
    # number of columns selected.
    @staticmethod
    def __transform_to_dict(column_names, query_result):
        transformed = []
        for row in query_result:
            dict_obj = {}
            for i in range(len(column_names)):
                column_name = column_names[i]
                value = row[i]
                dict_obj[column_name] = value
            transformed.append(dict_obj)
        return transformed
