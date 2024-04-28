from flask import Flask, request, render_template, Response, redirect,jsonify, make_response, g, redirect
from flask_mysqldb import MySQL
from functools import wraps
import os
import json
from utils.db import example_query

app = Flask(__name__)

app.config['SECRET_KEY'] = os.environ.get('SECRET', os.urandom(32))
app.config['MYSQL_HOST'] = os.environ.get('DBHOST', '')
app.config['MYSQL_USER'] = os.environ.get('DBUSER', '')
app.config['MYSQL_PASSWORD'] = os.environ.get('DBPASS', '')
app.config['MYSQL_DB'] = os.environ.get('DBSCHEMA', '')

mysql = MySQL(app)

@app.before_request
def add_mysql():
    g.mysql = mysql.connection.cursor()

def token_required(f):
    @wraps(f)
    def decorated(*args, **kwargs):
        pass
        return  f("something", *args, **kwargs)
    return decorated

@app.route('/', methods=['GET'])
def login():
    result = example_query()
    return render_template('home.html')


if __name__ == '__main__':
    app.run(host="0.0.0.0")


