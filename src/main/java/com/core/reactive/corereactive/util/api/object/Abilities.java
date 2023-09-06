package com.core.reactive.corereactive.util.api.object;

import com.fasterxml.jackson.annotation.JsonProperty;

public class Abilities{
    @JsonProperty("E")
    public E getE() {
        return this.e; }
    public void setE(E e) {
        this.e = e; }
    E e;
    @JsonProperty("Passive")
    public Passive getPassive() {
        return this.passive; }
    public void setPassive(Passive passive) {
        this.passive = passive; }
    Passive passive;
    @JsonProperty("Q")
    public Q getQ() {
        return this.q; }
    public void setQ(Q q) {
        this.q = q; }
    Q q;
    @JsonProperty("R")
    public R getR() {
        return this.r; }
    public void setR(R r) {
        this.r = r; }
    R r;
    @JsonProperty("W")
    public W getW() {
        return this.w; }
    public void setW(W w) {
        this.w = w; }
    W w;
}