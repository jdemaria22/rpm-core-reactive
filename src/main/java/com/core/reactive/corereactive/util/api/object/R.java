package com.core.reactive.corereactive.util.api.object;

import com.fasterxml.jackson.annotation.JsonProperty;

public class R{
        @JsonProperty("abilityLevel")
        public int getAbilityLevel() {
            return this.abilityLevel; }
        public void setAbilityLevel(int abilityLevel) {
            this.abilityLevel = abilityLevel; }
        int abilityLevel;
        @JsonProperty("displayName")
        public String getDisplayName() {
            return this.displayName; }
        public void setDisplayName(String displayName) {
            this.displayName = displayName; }
        String displayName;
        @JsonProperty("id")
        public String getId() {
            return this.id; }
        public void setId(String id) {
            this.id = id; }
        String id;
        @JsonProperty("rawDescription")
        public String getRawDescription() {
            return this.rawDescription; }
        public void setRawDescription(String rawDescription) {
            this.rawDescription = rawDescription; }
        String rawDescription;
        @JsonProperty("rawDisplayName")
        public String getRawDisplayName() {
            return this.rawDisplayName; }
        public void setRawDisplayName(String rawDisplayName) {
            this.rawDisplayName = rawDisplayName; }
        String rawDisplayName;
    }
