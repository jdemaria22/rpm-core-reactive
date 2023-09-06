package com.core.reactive.corereactive.util.api.object;

import com.fasterxml.jackson.annotation.JsonProperty;

public class Root{
        @JsonProperty("abilities")
        public Abilities getAbilities() {
            return this.abilities; }
        public void setAbilities(Abilities abilities) {
            this.abilities = abilities; }
        Abilities abilities;
        @JsonProperty("championStats")
        public ChampionStats getChampionStats() {
            return this.championStats; }
        public void setChampionStats(ChampionStats championStats) {
            this.championStats = championStats; }
        ChampionStats championStats;
        @JsonProperty("currentGold")
        public double getCurrentGold() {
            return this.currentGold; }
        public void setCurrentGold(double currentGold) {
            this.currentGold = currentGold; }
        double currentGold;
        @JsonProperty("fullRunes")
        public FullRunes getFullRunes() {
            return this.fullRunes; }
        public void setFullRunes(FullRunes fullRunes) {
            this.fullRunes = fullRunes; }
        FullRunes fullRunes;
        @JsonProperty("level")
        public int getLevel() {
            return this.level; }
        public void setLevel(int level) {
            this.level = level; }
        int level;
        @JsonProperty("summonerName")
        public String getSummonerName() {
            return this.summonerName; }
        public void setSummonerName(String summonerName) {
            this.summonerName = summonerName; }
        String summonerName;
        @JsonProperty("teamRelativeColors")
        public boolean getTeamRelativeColors() {
            return this.teamRelativeColors; }
        public void setTeamRelativeColors(boolean teamRelativeColors) {
            this.teamRelativeColors = teamRelativeColors; }
        boolean teamRelativeColors;
    }
