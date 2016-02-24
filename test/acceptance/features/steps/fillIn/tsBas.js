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

/* globals logg, pages, browser, JSON, Promise */

'use strict';
var tsBasUtkastPage = pages.intyg.ts.bas.utkast;

module.exports = {
	fillIn:function(intyg,cb){

      var promiseArr = [];
      promiseArr.push(tsBasUtkastPage.fillInKorkortstyper(intyg.korkortstyper, 'intygetAvserForm').then(function () {
        logg('OK - fillInKorkortstyper, ' + JSON.stringify(intyg.korkortstyper));
      }, function (reason) {
        cb('FEL, fillInKorkortstyper, ' + JSON.stringify(intyg.korkortstyper) + reason);
      }));

      promiseArr.push(tsBasUtkastPage.fillInIdentitetStyrktGenom(intyg.identitetStyrktGenom).then(function () {
        logg('OK - fillInIdentitetStyrktGenom:' + intyg.identitetStyrktGenom.toString() );
      }, function (reason) {
        cb('FEL, fillInIdentitetStyrktGenom,' + reason);
      }));

      // Synfunktioner
      browser.ignoreSynchronization = true;
      promiseArr.push(tsBasUtkastPage.fillInSynfunktioner(global.intyg).then(function () {
        logg('OK - fillInSynfunktioner');
      }, function (reason) {
        cb('FEL, fillInSynfunktioner,' + reason);
      }));

      promiseArr.push(tsBasUtkastPage.fillInHorselOchBalanssinne(intyg.horsel).then(function () {
        logg('OK - fillInHorselOchBalanssinne: ' + JSON.stringify(intyg.horsel));
      }, function (reason) {
        cb('FEL, fillInHorselOchBalanssinne,'+ JSON.stringify(intyg.horsel) + reason);
      }));
      promiseArr.push(tsBasUtkastPage.fillInRorelseorganensFunktioner(global.intyg.rorelseorganensFunktioner).then(function () {
        logg('OK - fillInRorelseorganensFunktioner, ' + JSON.stringify(global.intyg.rorelseorganensFunktioner));
      }, function (reason) {
        cb('FEL, fillInRorelseorganensFunktioner,'+ JSON.stringify(global.intyg.rorelseorganensFunktioner) + reason);
      }));
      promiseArr.push(tsBasUtkastPage.fillInHjartOchKarlsjukdomar(global.intyg).then(function () {
        logg('OK - fillInHjartOchKarlsjukdomar');
      }, function (reason) {
        cb('FEL, fillInHjartOchKarlsjukdomar,' + reason);
      }));
      promiseArr.push(tsBasUtkastPage.fillInDiabetes(intyg.diabetes).then(function () {
        logg('OK - fillInDiabetes: ' + JSON.stringify(intyg.diabetes));
      }, function (reason) {
        cb('FEL, fillInDiabetes:, ' + JSON.stringify(intyg.diabetes) + reason);
      }));
      // promiseArr.push(tsBasUtkastPage.fillInHorselOchBalanssinne(global.intyg).then(function () {
      //   logg('OK - fillInHorselOchBalanssinne');
      // }, function (reason) {
      //   cb('FEL, fillInHorselOchBalanssinne,' + reason);
      // }));
      promiseArr.push(tsBasUtkastPage.fillInNeurologiskaSjukdomar(global.intyg).then(function () {
        logg('OK - fillInNeurologiskaSjukdomar');
      }, function (reason) {
        cb('FEL, fillInNeurologiskaSjukdomar,' + reason);
      }));
      promiseArr.push(tsBasUtkastPage.fillInEpilepsi(global.intyg).then(function () {
        logg('OK - fillInEpilepsi');
      }, function (reason) {
        cb('FEL, fillInEpilepsi,' + reason);
      }));
      promiseArr.push(tsBasUtkastPage.fillInNjursjukdomar(global.intyg).then(function () {
        logg('OK - fillInNjursjukdomar');
      }, function (reason) {
        cb('FEL, fillInNjursjukdomar,' + reason);
      }));
      promiseArr.push(tsBasUtkastPage.fillInDemens(global.intyg).then(function () {
        logg('OK - fillInDemens');
      }, function (reason) {
        cb('FEL, fillInDemens,' + reason);
      }));
      promiseArr.push(tsBasUtkastPage.fillInSomnOchVakenhet(global.intyg).then(function () {
        logg('OK - fillInSomnOchVakenhet');
      }, function (reason) {
        cb('FEL, fillInSomnOchVakenhet,' + reason);
      }));
      promiseArr.push(tsBasUtkastPage.fillInAlkoholNarkotikaLakemedel(global.intyg).then(function () {
        logg('OK - fillInAlkoholNarkotikaLakemedel');
      }, function (reason) {
        cb('FEL, fillInAlkoholNarkotikaLakemedel,' + reason);
      }));
      promiseArr.push(tsBasUtkastPage.fillInPsykiska(global.intyg).then(function () {
        logg('OK - fillInPsykiska');
      }, function (reason) {
        cb('FEL, fillInPsykiska,' + reason);
      }));
      promiseArr.push(tsBasUtkastPage.fillInAdhd(global.intyg).then(function () {
        logg('OK - fillInAdhd');
      }, function (reason) {
        cb('FEL, fillInAdhd,' + reason);
      }));
      promiseArr.push(tsBasUtkastPage.fillInSjukhusvard(global.intyg).then(function () {
        logg('OK - fillInSjukhusvard');
      }, function (reason) {
        cb('FEL, fillInSjukhusvard,' + reason);
      }));

      promiseArr.push(tsBasUtkastPage.fillInOvrigMedicinering(global.intyg).then(function () {
        logg('OK - fillInOvrigMedicinering');
      }, function (reason) {
        cb('FEL, fillInOvrigMedicinering,' + reason);
      }));

      promiseArr.push(tsBasUtkastPage.fillInBedomning(intyg.bedomning).then(function () {
        logg('OK - fillInOvrigMedicinering');
      }, function (reason) {
        cb('FEL, fillInOvrigMedicinering,' + reason);
      }));

       return Promise.all(promiseArr)
       .then(function (value) {
          browser.ignoreSynchronization = false;
          cb();
        });
	}
};