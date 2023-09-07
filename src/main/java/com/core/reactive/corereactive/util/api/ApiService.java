package com.core.reactive.corereactive.util.api;

import com.core.reactive.corereactive.component.unitmanager.champion.Champion;
import com.core.reactive.corereactive.util.api.object.JsonActivePlayer;
import com.core.reactive.corereactive.util.api.object.JsonCommunityDragon;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.util.InsecureTrustManagerFactory;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.stereotype.Service;
import org.springframework.util.ObjectUtils;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.netty.http.client.HttpClient;

import java.math.BigDecimal;

@Service
@Slf4j
public class ApiService {
    private static final String URL_JSON_COMMUNITY = "https://raw.communitydragon.org/latest/game/data/characters/{id}/{id}.bin.json";
    public static final String TARGET = "{id}";
    public static final String CHARACTERS_ID_CHARACTERRECORDS_ROOT = "Characters/{id}/CharacterRecords/Root";
    public static final String ATTACK_SPEED = "attackSpeed";
    public static final String OVERRIDE_GAMEPLAY_COLLISION_RADIUS = "overrideGameplayCollisionRadius";
    public static final String BASIC_ATTACK = "basicAttack";
    public static final String M_ATTACK_DELAY_CAST_OFFSET_PERCENT = "mAttackDelayCastOffsetPercent";
    public static final String M_ATTACK_DELAY_CAST_OFFSET_PERCENT_ATTACK_SPEED_RATIO = "mAttackDelayCastOffsetPercentAttackSpeedRatio";
    private final WebClient webClient;
    private final WebClient webClientCom;
    public ApiService(WebClient.Builder webClientBuilder) {
        HttpClient httpClient = HttpClient.create().secure(t -> t.sslContext(this.sslContext()));
        this.webClient = webClientBuilder.baseUrl("https://127.0.0.1:2999/liveclientdata/activeplayer")
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .build();
        this.webClientCom = webClientBuilder
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .build();

    }

    @SneakyThrows
    private SslContext sslContext(){
        return SslContextBuilder
                .forClient()
                .trustManager(InsecureTrustManagerFactory.INSTANCE)
                .build();
    }

    public Mono<JsonActivePlayer> getJsonActivePlayer() {
        return this.webClient.get().retrieve().bodyToMono(JsonActivePlayer.class);
    }

    public Mono<JsonCommunityDragon> getJsonCommunityDragon(Champion champion) {
//        try {
            String uri = URL_JSON_COMMUNITY.replace(TARGET, champion.getName().toLowerCase());
            return this.webClient.get().uri(uri)
                    .retrieve().bodyToMono(String.class)
                    .flatMap(s -> Mono.fromCallable(() -> {
                        JSONObject jsonObject = new JSONObject(s);
                        JSONObject root = jsonObject.getJSONObject(CHARACTERS_ID_CHARACTERRECORDS_ROOT.replace(TARGET, champion.getName()));

                        String attackSpeed = this.get(ATTACK_SPEED, root);
                        BigDecimal attackSpeedValue = new BigDecimal(ObjectUtils.isEmpty(attackSpeed)?"0.0":attackSpeed);

                        String gameplayRadius = this.get(OVERRIDE_GAMEPLAY_COLLISION_RADIUS, root);
                        BigDecimal gameplayRadiusValue = new BigDecimal(ObjectUtils.isEmpty(gameplayRadius)?"65.0":gameplayRadius);

                        JSONObject basicAttack = root.getJSONObject(BASIC_ATTACK);
                        String mAttackDelayCastOffsetPercent = this.get(M_ATTACK_DELAY_CAST_OFFSET_PERCENT, basicAttack);
                        BigDecimal mAttackDelayCastOffsetPercentVal = new BigDecimal(ObjectUtils.isEmpty(mAttackDelayCastOffsetPercent)?"0.0":mAttackDelayCastOffsetPercent);
                        BigDecimal windUp = mAttackDelayCastOffsetPercentVal.add(new BigDecimal("0.3"));

                        String mAttackDelayCastOffsetPercentAttackSpeedRatio = this.get(M_ATTACK_DELAY_CAST_OFFSET_PERCENT_ATTACK_SPEED_RATIO, basicAttack);
                        BigDecimal mAttackDelayCastOffsetPercentAttackSpeedRatioValue = new BigDecimal(ObjectUtils.isEmpty(mAttackDelayCastOffsetPercentAttackSpeedRatio)?"0.0":mAttackDelayCastOffsetPercentAttackSpeedRatio);

                        JsonCommunityDragon jsonCommunityDragon = JsonCommunityDragon.builder()
                                .attackSpeed(attackSpeedValue)
                                .gameplayRadius(gameplayRadiusValue)
                                .windUp(windUp)
                                .windupMod(mAttackDelayCastOffsetPercentAttackSpeedRatioValue)
                                .build();
//                    log.info("jsonCommunityDragon {}", jsonCommunityDragon);
                        return jsonCommunityDragon;
                    }));
//        } catch (Exception e) {
//            return Mono.just(JsonCommunityDragon.builder().build());
//        }

    }

    private String get(String key, JSONObject root) {
        try {
            return root.get(key).toString();
        } catch (JSONException j) {
            return null;
        }
    }

}
