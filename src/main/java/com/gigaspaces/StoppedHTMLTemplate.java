package com.gigaspaces;

import com.gigaspaces.Loop;
import com.gigaspaces.actions.NotifyBeforeStopAction;
import com.gigaspaces.actions.StopAction;
import org.antlr.stringtemplate.StringTemplate;
import org.antlr.stringtemplate.StringTemplateGroup;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class StoppedHTMLTemplate {
    @SuppressWarnings("WeakerAccess")
    final static Logger logger = LoggerFactory.getLogger(Loop.class);
    private StopAction action;


    @SuppressWarnings("WeakerAccess")
    public StoppedHTMLTemplate(StopAction action) {
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
        return group.getInstanceOf("shutdown-template");
    }

}
