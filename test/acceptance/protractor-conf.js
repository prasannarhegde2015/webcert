/*
 * Copyright (C) 2016 Inera AB (http://www.inera.se)
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

/*global
browser, JSON
*/
'use strict';
var winston = require('winston');
var fs = require('fs');

exports.config = {
    baseUrl: process.env.WEBCERT_URL,
    allScriptsTimeout: 100000,
    getPageTimeout: 20000,
    seleniumAddress: 'http://selenium1.nordicmedtest.se:4444/wd/hub',
    framework: 'custom',

    // path relative to the current config file
    frameworkPath: require.resolve('protractor-cucumber-framework'),
    specs: [
        'features/*.feature'
    ],
    capabilities: {
        shardTestFiles: false,
        maxInstances: 1,
        browserName: 'firefox',
        // browserName: 'internet explorer',
        // 'phantomjs.binary.path': './node_modules/karma-phantomjs-launcher/node_modules/phantomjs/bin/phantomjs',
        //'phantomjs.cli.args': '--debug=true --webdriver --webdriver-logfile=webdriver.log --webdriver-loglevel=DEBUG',
        version: '',
        platform: 'ANY'
    },
    cucumberOpts: {
        format: ['json:./acceptance/report/' + (Math.ceil(100000000 * Math.random())) + '_acc_results.json', 'pretty'],
        require: ['features/steps/**/*.js', 'features/support/**/*.js']
    },
    onPrepare: function() {
        browser.manage().window().setSize(1600, 1000);
        //http://chaijs.com/
        global.chai = require('chai');

        //https://github.com/domenic/chai-as-promised/
        global.chaiAsPromised = require('chai-as-promised');
        global.chai.use(global.chaiAsPromised);

        global.expect = global.chai.expect;
        global.assert = global.chai.assert;
        global.should = global.chai.should();

        var wcTestTools = require('webcert-testtools');

        global.wcTestTools = wcTestTools;
        global.testdata = wcTestTools.testdata;
        global.pages = wcTestTools.pages;
        global.person = {};

        browser.ignoreSynchronization = false;
        browser.baseUrl = process.env.WEBCERT_URL;

        //logger.infoing
        global.logger = new(winston.Logger)({
            transports: [
                new(winston.transports.Console)({
                    colorize: true,
                    timestamp: formatLocalDate,
                    formatter: function(options) {
                        // Return string will be passed to logger.
                        return options.timestamp() + ' ' + options.level.toUpperCase() + ' ' + (undefined !== options.message ? options.message : '') +
                            (options.meta && Object.keys(options.meta).length ? '\n\t' + JSON.stringify(options.meta) : '');
                    }
                })
            ]
        });

        //Set window size
        browser.manage().window().setSize(1600, 1000);

        //Strunta i om servern inte kan bekräfta dess identitet
        process.env.NODE_TLS_REJECT_UNAUTHORIZED = '0';

        // Disable animations so e2e tests run more quickly
        var disableNgAnimate = function() {
            angular.module('disableNgAnimate', []).run(['$animate',
                function($animate) {
                    $animate.enabled(false);
                }
            ]);
        };
        browser.addMockModule('disableNgAnimate', disableNgAnimate);
    },
    onComplete: function() {

        // Kontrollera externa länkar på sidan som samlats ihop under scenario.
        if (global.externalPageLinks.length > 0) {
            var linksArr = [];

            fs.readFile(browser.params.externalLinksFile, function(err, data) {
                if (err) {
                    if (err.code === 'ENOENT') {
                        console.error(browser.params.externalLinksFile + ' finns inte');
                        return;
                    } else {
                        throw err;
                    }
                } else {
                    linksArr = data.split(',');

                    global.externalPageLinks.forEach(function(item) {
                        if (item.indexOf() === -1) {
                            linksArr.push(item);
                        }
                    });
                }
            });

            if (linksArr.length === 0) {
                linksArr = global.externalPageLinks;
            }

            fs.writeFile(browser.params.externalLinksFile, linksArr.join(',') + ',', function(err) {
                if (err) {
                    throw err;
                }
            });

        }
    }
};


function formatLocalDate() {
    var now = new Date(),
        tzo = -now.getTimezoneOffset(),
        dif = tzo >= 0 ? '+' : '-',
        pad = function(num) {
            var norm = Math.abs(Math.floor(num));
            return (norm < 10 ? '0' : '') + norm;
        };
    return now.getFullYear() + '-' + pad(now.getMonth() + 1) + '-' + pad(now.getDate()) + 'T' + pad(now.getHours()) + ':' + pad(now.getMinutes()) + ':' + pad(now.getSeconds()) + dif + pad(tzo / 60) +
        ':' + pad(tzo % 60);
}
