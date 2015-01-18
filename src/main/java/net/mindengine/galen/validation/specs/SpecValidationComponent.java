/*******************************************************************************
* Copyright 2015 Ivan Shubin http://mindengine.net
* 
* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at
* 
*   http://www.apache.org/licenses/LICENSE-2.0
* 
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
******************************************************************************/
package net.mindengine.galen.validation.specs;

import java.io.IOException;
import java.util.List;

import net.mindengine.galen.browser.Browser;
import net.mindengine.galen.page.Page;
import net.mindengine.galen.page.PageElement;
import net.mindengine.galen.specs.SpecComponent;
import net.mindengine.galen.specs.page.Locator;
import net.mindengine.galen.specs.reader.page.PageSpec;
import net.mindengine.galen.specs.reader.page.PageSpecReader;
import net.mindengine.galen.specs.reader.page.SectionFilter;
import net.mindengine.galen.validation.*;

public class SpecValidationComponent extends SpecValidation<SpecComponent> {

    @Override
    public void check(PageValidation pageValidation, String objectName, SpecComponent spec) throws ValidationErrorException {
        PageElement mainObject = pageValidation.findPageElement(objectName);
        checkAvailability(mainObject, objectName);

        List<ValidationError> errors;

        if (spec.isFrame()) {
            errors = checkInsideFrame(mainObject, pageValidation, spec);
        }
        else {
            errors = checkInsideNormalWebElement(pageValidation, objectName, spec);
        }

        if (errors != null && errors.size() > 0) {
            throw new ValidationErrorException("Child component spec contains " + errors.size() + " errors");
        }

    }

    private List<ValidationError> checkInsideFrame(PageElement mainObject, PageValidation pageValidation, SpecComponent spec) {
        Page page = pageValidation.getPage();

        Page framePage = page.createFrameContext(mainObject);

        List<ValidationError> errors = checkInsidePage(pageValidation.getBrowser(), framePage, spec,
                pageValidation.getSectionFilter(), pageValidation.getValidationListener());

        if (spec.isFrame()) {
            page.switchToParentFrame();
        }

        return errors;
    }


    private List<ValidationError> checkInsidePage(Browser browser, Page page, SpecComponent spec,
                                                  SectionFilter sectionFilter, ValidationListener validationListener) {
        PageSpecReader pageSpecReader = new PageSpecReader(spec.getProperties(), page);

        PageSpec componentPageSpec;
        try {
            componentPageSpec = pageSpecReader.read(spec.getSpecPath());
        } catch (IOException e) {
            throw new RuntimeException(e.getMessage(), e);
        }

        SectionValidation sectionValidation = new SectionValidation(componentPageSpec.findSections(sectionFilter),
                new PageValidation(browser, page, componentPageSpec, validationListener, sectionFilter),
                validationListener);

        return sectionValidation.check();
    }

    private List<ValidationError> checkInsideNormalWebElement(PageValidation pageValidation, String objectName, SpecComponent spec) {
        Locator mainObjectLocator = pageValidation.getPageSpec().getObjectLocator(objectName);
        Page objectContextPage = pageValidation.getPage().createObjectContextPage(mainObjectLocator);

        return checkInsidePage(pageValidation.getBrowser(), objectContextPage, spec,
                pageValidation.getSectionFilter(), pageValidation.getValidationListener());
    }

}
