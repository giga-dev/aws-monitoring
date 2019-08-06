package com.gigaspaces;

import com.gigaspaces.actions.NotifyBeforeStopAction;
import io.vavr.collection.HashSet;
import org.antlr.stringtemplate.StringTemplate;
import org.antlr.stringtemplate.StringTemplateGroup;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class WarnHTMLTemplate {
    @SuppressWarnings("WeakerAccess")
    final static Logger logger = LoggerFactory.getLogger(Loop.class);
    private NotifyBeforeStopAction action;


    @SuppressWarnings("WeakerAccess")
    public WarnHTMLTemplate(NotifyBeforeStopAction action) {
        this.action = action;
    }

    @SuppressWarnings("WeakerAccess")
    public String formatHTMLBody(){
        StringTemplate bodyTemplate = createHtmlTemplate();
        bodyTemplate.setAttribute("action", action);
        bodyTemplate.setAttribute("subject", action.getSubject());
        bodyTemplate.setAttribute("instance", action.getInstance());
        return bodyTemplate.toString();
    }

    private StringTemplate createHtmlTemplate() {
        StringTemplateGroup group = new StringTemplateGroup("html-templates");
        return group.getInstanceOf("warn-template");
    }

//    public static void main(String[] args) {
//        WarnHTMLTemplate template = new WarnHTMLTemplate(Diff.create(HashSet.empty(), HashSet.empty()));
//        String message = template.formatHTMLBody();
//        logger.info("message is {}", message);
//    }
}
