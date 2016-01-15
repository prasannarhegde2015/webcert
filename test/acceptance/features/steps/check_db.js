/*
 * Copyright (C) 2015 Inera AB (http://www.inera.se)
 *
 * This file is part of sklintyg (https://github.com/sklintyg).
 *
 * sklintyg is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * sklintyg is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
 
/* globals pages, protractor, intyg, logg, browser,should*/

'use strict';

var mysql = require('mysql');

module.exports = function () {
	this.Given(/^ska spår av utkastet inte finnas i databasen$/, function (callback) {
    
    if(!process.env.DATABASE_PASSWORD){
      callback('Miljövariabel DATABASE_PASSWORD saknas för DATABASE_USER:'+process.env.DATABASE_USER);
    }
    else{
      var dbName = process.env.DATABASE_NAME;
      var connection = mysql.createConnection({
        host  : process.env.DATABASE_HOST,
        user  : process.env.DATABASE_USER,
        password  : process.env.DATABASE_PASSWORD,
        database  : dbName
      });

      connection.connect();
      connection.query('SELECT COUNT(*) AS Counter FROM '+dbName+'.INTYG WHERE '+dbName+'.INTYG.INTYGS_ID = \"'+intyg.id+'\";', function(err, rows, fields){
        should.not.exist(err);

        logg('Intygs id: ' + intyg.id);
        logg('Från databas:');
        logg(JSON.stringify(rows));

        var radix = 10; //for parseInt
        expect(parseInt(rows[0].Counter,radix)).to.equal(0);
        callback();
      });
      connection.end();
    }
});
};