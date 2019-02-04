package com.github.danielfernandez.matchday.web.view;

import java.util.Arrays;

import com.samskivert.mustache.Mustache;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.mustache.MustacheProperties;
import org.springframework.boot.web.reactive.result.view.MustacheViewResolver;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.result.view.AbstractUrlBasedView;

@Configuration
public class MustacheConfiguration {

    @Autowired
    private Mustache.Compiler compiler;

    @Autowired
    private MustacheProperties mustache;

    @Bean
    public MustacheViewResolver reactiveMustacheViewResolver() {
        MustacheViewResolver resolver = new MustacheViewResolver(compiler) {
            {
                setSupportedMediaTypes(Arrays.asList(MediaType.TEXT_HTML, MediaType.TEXT_EVENT_STREAM));
            }
            @Override
            protected Class<?> requiredViewClass() {
                return ReactiveMustacheView.class;
            }

            @Override
            protected AbstractUrlBasedView createView(String viewName) {
                ReactiveMustacheView view = (ReactiveMustacheView) super.createView(
                        viewName);
                view.setCompiler(compiler);
                view.setCharset(mustache.getCharsetName());
                return view;
            }
        };
        resolver.setPrefix(this.mustache.getPrefix());
        resolver.setSuffix(this.mustache.getSuffix());
        resolver.setViewNames(this.mustache.getViewNames());
        resolver.setRequestContextAttribute(this.mustache.getRequestContextAttribute());
        resolver.setCharset(this.mustache.getCharsetName());
        resolver.setOrder(Ordered.LOWEST_PRECEDENCE - 10);
        return resolver;
    }

}