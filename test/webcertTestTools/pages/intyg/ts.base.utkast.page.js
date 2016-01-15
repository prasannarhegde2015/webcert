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

/**
 * Created by bennysce on 09/06/15.
 */
/*globals element,by,browser*/
'use strict';

var pageHelpers = require('./../pageHelper.util.js');
var BaseUtkast = require('./base.utkast.page.js');

var BaseTsUtkast = BaseUtkast._extend({
    init: function init() {
        init._super.call(this);

        this.intygType = null; // overridden by children

        this.korkortsTyperChecks = element(by.id('intygetAvserForm')).all(by.css('label.checkbox'));

        this.identitetForm = element(by.id('identitetForm'));

        this.bedomning = {
            form: element(by.id('bedomningForm')),
            yes: element(by.id('bedomningy')),
            no: element(by.id('bedomningn'))
        };
        this.bedomningKorkortsTyperChecks = this.bedomning.form.all(by.css('label.checkbox'));

        this.kommentar = element(by.id('kommentar'));
    },
    get: function get(intygId) {
        get._super.call(this, this.intygType, intygId);
    },
    fillInKorkortstyper: function(typer) {
        pageHelpers.clickAll(this.korkortsTyperChecks, typer);
    },
    fillInIdentitetStyrktGenom: function(idtyp) {
        this.identitetForm.element(by.cssContainingText('label.radio', idtyp)).sendKeys(protractor.Key.SPACE);
    },
    fillInBedomning: function(bedomningObj) {
        element(by.id(bedomningObj.stallningstagande)).sendKeys(protractor.Key.SPACE);
        pageHelpers.clickAll(this.bedomningKorkortsTyperChecks, bedomningObj.behorigheter);
    },
    fillInOvrigKommentar: function(utkast) {
        this.kommentar.sendKeys(utkast.kommentar);
    }
});

module.exports = BaseTsUtkast;