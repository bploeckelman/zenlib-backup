package zendo.games.zenlib;

public interface Game {
    void init();
    void update(float dt);
    void render();
    void shutdown();
}
