/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package org.iis.plagiarismdetector.core.NamedEntityTagger;

import java.util.Objects;

/**
 *
 * @author mosi
 */
public class Candidate {

    private String label;
    private String uri;
    private String contextualScore;
    private String percentageOfSecondRa;
    private String support;
    private String priorScore;
    private String finalScore;
    private String types;
    private String wikipediaURL;
    private String freebaseURL;
    
    public Candidate(){
        
    }
    public Candidate(String label, String uri, String contextualScore, String percentageOfSecondRa, String support, String priorScore, String finalScore, String types, String wikipediaURL, String freebaseURL) {
        this.label = label;
        this.uri = uri;
        this.contextualScore = contextualScore;
        this.percentageOfSecondRa = percentageOfSecondRa;
        this.support = support;
        this.priorScore = priorScore;
        this.finalScore = finalScore;
        this.types = types;
        this.wikipediaURL = wikipediaURL;
        this.freebaseURL = freebaseURL;
    }

    public String getFreebaseURL() {
        return freebaseURL;
    }

    public void setFreebaseURL(String freebaseURL) {
        this.freebaseURL = freebaseURL;
    }

    
    
    public String getLabel() {
        return label;
    }

    public String getUri() {
        return uri;
    }

    public String getContextualScore() {
        return contextualScore;
    }

    public String getPercentageOfSecondRa() {
        return percentageOfSecondRa;
    }

    public String getSupport() {
        return support;
    }

    public String getPriorScore() {
        return priorScore;
    }

    public String getFinalScore() {
        return finalScore;
    }

    public String getTypes() {
        return types;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public void setUri(String uri) {
        this.uri = uri;
    }

    public void setContextualScore(String contextualScore) {
        this.contextualScore = contextualScore;
    }

    public void setPercentageOfSecondRa(String percentageOfSecondRa) {
        this.percentageOfSecondRa = percentageOfSecondRa;
    }

    public void setSupport(String support) {
        this.support = support;
    }

    public void setPriorScore(String priorScore) {
        this.priorScore = priorScore;
    }

    public void setFinalScore(String finalScore) {
        this.finalScore = finalScore;
    }

    public void setTypes(String types) {
        this.types = types;
    }

    public String getWikipediaURL() {
        return wikipediaURL;
    }

    public void setWikipediaURL(String wikipediaURL) {
        this.wikipediaURL = wikipediaURL;
    }
    
    @Override
    public int hashCode() {
        int hash = 5;
        hash = 53 * hash + Objects.hashCode(this.uri);
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final Candidate other = (Candidate) obj;
        if (!Objects.equals(this.uri, other.uri)) {
            return false;
        }
        return true;
    }
    
    


    
}
