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

/* global module, Promise*/
require('path');
var request = require('request');

module.exports = function(grunt) {
    'use strict';

    grunt.loadNpmTasks('grunt-protractor-runner');
    grunt.loadNpmTasks('grunt-protractor-webdriver');
    grunt.loadNpmTasks('grunt-env');
    grunt.loadNpmTasks('grunt-contrib-jshint');
    grunt.loadNpmTasks("grunt-jsbeautifier");
    grunt.loadNpmTasks('grunt-force-task');

    var devSuite = grunt.option('suite') || 'app';
    grunt.initConfig({
        jshint: {
            acc: [
                'acceptance/features/steps/*.js',
                'acceptance/features/steps/**/*.js' //,
                //'webcertTestTools/**/*.js',
                //'webcertTestTools/*.js'
            ],
            options: {
                force: false,
                jshintrc: '../../../gradle-intyg-plugin/src/main/resources/jshint/jshintrc'
            }
        },
        jsbeautifier: {
            verify: {
                src: [
                    'acceptance/features/steps/*.js',
                    'acceptance/features/steps/**/*.js' //,
                    //'webcertTestTools/**/*.js',
                    //'webcertTestTools/*.js'
                ],
                options: {
                    mode: 'VERIFY_ONLY',
                    config: '../.jsbeautifyrc'
                }
            },
            modify: {
                src: [
                    'acceptance/features/steps/*.js',
                    'acceptance/features/steps/**/*.js',
                    'webcertTestTools/**/*.js',
                    'webcertTestTools/*.js'
                ],
                options: {
                    mode: 'VERIFY_AND_WRITE',
                    config: '../.jsbeautifyrc'
                }
            }
        },
        env: grunt.file.readJSON('../webcertTestTools/envConfig.json'),
        protractor: {
            options: {
                //configFile: './protractor.cli.conf.js', // Target-specific config file
                keepAlive: false, // If false, the grunt process stops when the test fails.
                noColor: false, // If true, protractor will not use colors in its output.
                args: {
                    // Arguments passed to the command
                }
            },
            acc: {
                options: {
                    configFile: 'protractor-conf.js',
                    args: {
                        params: {
                            // En fil där cucumber fyller i alla externa länkar som hittas.
                            externalLinksFile: 'external_links.txt'
                        }
                    }
                },
                partialReportPattern: 'node_modules/common-testtools/cucumber-html-report/*_acc_results.json',
                reportFile: 'node_modules/common-testtools/cucumber-html-report/acc_results.json'
            }
        },

        protractor_webdriver: { // jshint ignore:line
            options: {
                // Task-specific options go here.
            }
        }

    });

    grunt.registerTask('checkExternalLinks', 'Kontrollerar externa länkar som hittats under testerna', function() {
        var done = this.async();
        var externalFiles = grunt.file.read(grunt.config.get('protractor.acc.options.args.params.externalLinksFile'));
        var externalFilesArr = externalFiles.split(',').filter(function(item) {
            return item !== '';
        });

        var promiseArr = externalFilesArr.map(function(link) {
            var request_options = link;
            if (link.indexOf('sjunet.org') !== -1) {
                request_options = {
                    url: link,
                    strictSSL: false
                };
            }

            return new Promise(function(resolve, reject) {
                request(request_options, function(error, response) {
                    if (error) {
                        reject('En extern länk genererade ett fel: ' + error + 'länk: ' + link);
                    } else {
                        if (response.statusCode !== 200) {
                            reject('En extern länk returnerade en oönskad statuskod: ' + response.statusCode + ' , länk: ' + link);
                        } else {
                            resolve('Den här länken fungerar: ' + response.statusCode + ' , länk: ' + link);
                        }
                    }
                });
            });
        });

        // Mappa Promises från länkkontrollen till objekt som innehåller resultat samt resultat-text
        // för att undvika fail-fast (vi vill se alla länkar som går fel inte bara den första).
        var toResultObject = function(promise) {
            return promise
                .then(result => ({
                    success: true,
                    result
                }))
                .catch(error => ({
                    success: false,
                    error
                }));
        };

        Promise.all(promiseArr.map(toResultObject)).then(function(values) {
            var errors = 0;

            for (var i = 0; i < values.length; ++i) {
                if (!values[i].success) {
                    console.log('ERR: ' + values[i].error);
                    errors++;
                } else {
                    console.log(values[i].result);
                }
            }

            if (errors > 0) {
                grunt.fail.warn('Hittade ett eller flera fel i task checkExternalLinks');
            }
            done();
        });
    });

    grunt.registerTask('genReport', 'Genererar rapport från testkörningen', function() {
        var files = grunt.file.expand(grunt.config.get('protractor.acc.partialReportPattern'));
        var combinedReport = '[';
        files.forEach(function(item, index) {
            var fileText = grunt.file.read(item);
            // Ibland är delrapporter tomma eller innehaller endast en []. 
            // Hoppa over dessa.
            if (fileText !== '[]' && fileText !== '') {
                combinedReport += fileText.substring(1, (fileText.length - 2));

                if (index < files.length - 1) {
                    combinedReport += ',';
                }
            } else {
                grunt.log.subhead(fileText);
            }
        });
        combinedReport += ']';
        grunt.file.write(grunt.config.get('protractor.acc.reportFile'), combinedReport);

        files.forEach(function(item) {
            grunt.file.delete(item);
        });

        // Gör en kontroll om vi fick något fel i tidigare task. Denna hantering finns pga att vi tvingades köra med 'force' i 
        // task 'protractor:acc' då ett eventuellt fail i testfall i protractor-steget hindrar den här tasken från att köra 
        // och vi vill ha en rapport oavsett om ett testfall har gått fel eller inte. 
        if (grunt.fail.errorcount > 0) {
            grunt.log.subhead('Tidigare eller nuvarande task innehöll ett felmeddelande (errorcount =' + grunt.fail.errorcount + ')');
            grunt.fail.warn('Hittade ett fel i task force:protractor:acc');
        }
    });

    // Run: 'grunt acc:ip20'
    grunt.task.registerTask('acc', 'Task för att köra acceptanstest', function(environment) {

        //Miljö
        if (!environment) {
            var defaultEnv = 'ip30';
            grunt.log.subhead('Ingen miljö vald, använder ' + defaultEnv + '-miljön..');
            environment = defaultEnv;
        }

        if (grunt.option('gridnodeinstances')) {
            if (grunt.option('gridnodeinstances') > 1) {
                grunt.config.set('protractor.acc.options.args.capabilities.shardTestFiles', true);
                grunt.config.set('protractor.acc.options.args.capabilities.maxInstances', grunt.option('gridnodeinstances'));
            }
        }

        // Ange taggar som grunt.option istället for argument till task. Flexiblare när det gäller att
        // kombinera OCH och ELLER operatorer.
        // https://github.com/cucumber/cucumber/wiki/Tags
        var tagsArray = ['~@notReady', '~@waitingForFix'];
        if (grunt.option('tags')) {
            tagsArray = grunt.option('tags').split(',');
            tagsArray.forEach(function(tag, index) {
                tagsArray[index] = tagsArray[index].replace(' ', ',');
            });


            // Filtrera bort de feature-filer som inte har några scenarios med valda taggar om parallella tester 
            // ska koras (dvs. selenium-grid). 
            if (grunt.option('gridnodeinstances')) {

                var files = grunt.file.expand('acceptance/features/*.feature');
                var featureFiles = [];

                files.forEach(function(filePath) {

                    var fileText = grunt.file.read(filePath);

                    tagsArray.forEach(function(currentTag) {
                        if (fileText.indexOf(currentTag) > -1 && featureFiles.indexOf(filePath) == -1) {
                            featureFiles.push(filePath);
                        }
                    });
                });

                if (featureFiles.length === 0) {
                    grunt.fail.warn('Hittade inget scenario som hade någon av taggarna som specificerats');
                } else {
                    grunt.config.set('protractor.acc.options.args.specs', featureFiles);
                }
            }

        }
        grunt.log.subhead('Taggar:' + tagsArray);
        grunt.config.set('protractor.acc.options.args.cucumberOpts.tags', tagsArray);

        //Tasks
        var tasks = [];
        if (!grunt.option('CI')) {
            tasks = ['jshint:acc', 'jsbeautifier:verify'];
        }

        if (grunt.file.exists(grunt.config.get('protractor.acc.options.args.params.externalLinksFile'))) {
            grunt.file.delete(grunt.config.get('protractor.acc.options.args.params.externalLinksFile'));
        }

        tasks.push('env:' + environment);
        tasks.push('protractor_webdriver');
        tasks.push('force:protractor:acc');
        tasks.push('genReport');
        grunt.task.run(tasks);
    });
};
