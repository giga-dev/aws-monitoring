package com.gigaspaces;

import io.vavr.collection.HashSet;
import org.antlr.stringtemplate.StringTemplate;
import org.antlr.stringtemplate.StringTemplateGroup;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class HTMLTemplate {
    @SuppressWarnings("WeakerAccess")
    final static Logger logger = LoggerFactory.getLogger(Loop.class);

    private Diff diff;

    @SuppressWarnings("WeakerAccess")
    public HTMLTemplate(Diff diff) {
        this.diff = diff;
    }

    @SuppressWarnings("WeakerAccess")
    public String formatHTMLBody(){
        StringTemplate bodyTemplate = createHtmlTemplate();
        bodyTemplate.setAttribute("added", diff.getAdded().toJavaList());
        bodyTemplate.setAttribute("removed", diff.getRemoved().toJavaList());
        bodyTemplate.setAttribute("unchanged", diff.getUnchanged().toJavaList());
        bodyTemplate.setAttribute("diff", diff);
        return bodyTemplate.toString();
    }

    private StringTemplate createHtmlTemplate() {
        StringTemplateGroup group = new StringTemplateGroup("html-templates");
        return group.getInstanceOf("body-template");
    }

    public static void main(String[] args) {
        HTMLTemplate template = new HTMLTemplate(Diff.create(HashSet.empty(), HashSet.empty()));
        String message = template.formatHTMLBody();
        logger.info("message is {}", message);
    }
}
