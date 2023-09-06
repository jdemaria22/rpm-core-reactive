package com.core.reactive.corereactive.util.api.object;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.ArrayList;

public class FullRunes{
        @JsonProperty("generalRunes")
        public ArrayList<GeneralRune> getGeneralRunes() {
            return this.generalRunes; }
        public void setGeneralRunes(ArrayList<GeneralRune> generalRunes) {
            this.generalRunes = generalRunes; }
        ArrayList<GeneralRune> generalRunes;
        @JsonProperty("keystone")
        public Keystone getKeystone() {
            return this.keystone; }
        public void setKeystone(Keystone keystone) {
            this.keystone = keystone; }
        Keystone keystone;
        @JsonProperty("primaryRuneTree")
        public PrimaryRuneTree getPrimaryRuneTree() {
            return this.primaryRuneTree; }
        public void setPrimaryRuneTree(PrimaryRuneTree primaryRuneTree) {
            this.primaryRuneTree = primaryRuneTree; }
        PrimaryRuneTree primaryRuneTree;
        @JsonProperty("secondaryRuneTree")
        public SecondaryRuneTree getSecondaryRuneTree() {
            return this.secondaryRuneTree; }
        public void setSecondaryRuneTree(SecondaryRuneTree secondaryRuneTree) {
            this.secondaryRuneTree = secondaryRuneTree; }
        SecondaryRuneTree secondaryRuneTree;
        @JsonProperty("statRunes")
        public ArrayList<StatRune> getStatRunes() {
            return this.statRunes; }
        public void setStatRunes(ArrayList<StatRune> statRunes) {
            this.statRunes = statRunes; }
        ArrayList<StatRune> statRunes;
    }
