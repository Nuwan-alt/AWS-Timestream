package org.ohlcTS.models;

public class TestModel {

    int main;
    String name;
    int type;
    String version;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getType() {
        return type;
    }

    public void setType(int type) {
        this.type = type;
    }

    public String getVersion() {
        return version;
    }

    public int getMain() {
        return main;
    }

    public void setMain(int main) {
        this.main = main;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public TestModel(String name, int type, String version, int main) {
        this.name = name;
        this.type = type;
        this.version = version;
        this.main = main;
    }


}
