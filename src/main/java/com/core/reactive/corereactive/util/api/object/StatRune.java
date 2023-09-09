package com.core.reactive.corereactive.util.api.object;

import com.fasterxml.jackson.annotation.JsonProperty;

public class StatRune{
        @JsonProperty("id")
        public int getId() {
            return this.id; }
        public void setId(int id) {
            this.id = id; }
        int id;
        @JsonProperty("rawDescription")
        public String getRawDescription() {
            return this.rawDescription; }
        public void setRawDescription(String rawDescription) {
            this.rawDescription = rawDescription; }
        String rawDescription;
    }
