package org.levimc.launcher.core.mods.inbuilt;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class RuntimeConfigSchema {
    public static final class Category {
        public final String id;
        public final String title;
        public final String description;

        Category(String id, String title, String description) {
            this.id = id;
            this.title = title;
            this.description = description;
        }
    }

    public static final class Option {
        public final String value;
        public final String label;
        public final String description;
        public final String key;
        public final boolean disabled;
        public final String currentValue;
        public final boolean hasCurrentValue;

        Option(String value, String label, String description, String key, boolean disabled,
               String currentValue, boolean hasCurrentValue) {
            this.value = value;
            this.label = label;
            this.description = description;
            this.key = key;
            this.disabled = disabled;
            this.currentValue = currentValue;
            this.hasCurrentValue = hasCurrentValue;
        }
    }

    public static final class Condition {
        public final String key;
        public final String op;
        public final String value;

        Condition(String key, String op, String value) {
            this.key = key;
            this.op = op;
            this.value = value;
        }
    }

    public static final class Node {
        public final String id;
        public final String key;
        public final String category;
        public final String section;
        public final String title;
        public final String description;
        public final String type;
        public final String style;
        public final String defaultValue;
        public final String currentValue;
        public final boolean hasCurrentValue;
        public final String minValue;
        public final String maxValue;
        public final String step;
        public final String unit;
        public final String placeholder;
        public final String actionValue;
        public final int maxLength;
        public final boolean advanced;
        public final boolean disabled;
        public final boolean searchable;
        public final boolean allowReorder;
        public final boolean colorAlpha;
        public final boolean collapsible;
        public final List<Option> options;
        public final List<Condition> visibleWhen;
        public final List<Condition> enabledWhen;
        public final String signature;

        Node(JSONObject obj) {
            id = obj.optString("id", "");
            key = obj.optString("key", "");
            category = obj.optString("category", "");
            section = obj.optString("section", "");
            title = obj.optString("title", id);
            description = obj.optString("description", "");
            type = obj.optString("type", "info");
            style = obj.optString("style", "auto");
            defaultValue = obj.optString("default_value", "");
            hasCurrentValue = obj.has("current_value");
            currentValue = obj.optString("current_value", "");
            minValue = obj.optString("min_value", "");
            maxValue = obj.optString("max_value", "");
            step = obj.optString("step", "");
            unit = obj.optString("unit", "");
            placeholder = obj.optString("placeholder", "");
            actionValue = obj.optString("action_value", "true");
            maxLength = obj.optInt("max_length", 0);
            advanced = obj.optBoolean("advanced", false);
            disabled = obj.optBoolean("disabled", false);
            searchable = obj.optBoolean("searchable", false);
            allowReorder = obj.optBoolean("allow_reorder", false);
            colorAlpha = obj.optBoolean("color_alpha", true);
            collapsible = obj.optBoolean("collapsible", false);
            options = parseOptions(obj.optJSONArray("options"));
            visibleWhen = parseConditions(obj.optJSONArray("visible_when"));
            enabledWhen = parseConditions(obj.optJSONArray("enabled_when"));
            signature = obj.toString();
        }
    }

    public final int version;
    public final String defaultCategory;
    public final List<Category> categories;
    public final List<Node> nodes;

    private RuntimeConfigSchema(int version, String defaultCategory,
                                List<Category> categories, List<Node> nodes) {
        this.version = version;
        this.defaultCategory = defaultCategory;
        this.categories = Collections.unmodifiableList(categories);
        this.nodes = Collections.unmodifiableList(nodes);
    }

    public static RuntimeConfigSchema parse(String json) {
        if (json == null || json.trim().isEmpty()) return null;
        try {
            JSONObject root = new JSONObject(json);
            if (root.optInt("version", 0) != 2) return null;
            List<Category> categories = new ArrayList<>();
            JSONArray categoryArray = root.optJSONArray("categories");
            if (categoryArray != null) {
                for (int i = 0; i < categoryArray.length(); i++) {
                    JSONObject obj = categoryArray.optJSONObject(i);
                    if (obj == null) continue;
                    String id = obj.optString("id", "").trim();
                    if (id.isEmpty()) continue;
                    categories.add(new Category(id, obj.optString("title", id),
                            obj.optString("description", "")));
                }
            }
            List<Node> nodes = new ArrayList<>();
            JSONArray nodeArray = root.optJSONArray("nodes");
            if (nodeArray != null) {
                for (int i = 0; i < nodeArray.length(); i++) {
                    JSONObject obj = nodeArray.optJSONObject(i);
                    if (obj == null) continue;
                    Node node = new Node(obj);
                    if (!node.id.isEmpty()) nodes.add(node);
                }
            }
            return new RuntimeConfigSchema(2, root.optString("default_category", ""), categories, nodes);
        } catch (Exception ignored) {
            return null;
        }
    }

    private static List<Option> parseOptions(JSONArray array) {
        if (array == null) return Collections.emptyList();
        List<Option> result = new ArrayList<>();
        for (int i = 0; i < array.length(); i++) {
            JSONObject obj = array.optJSONObject(i);
            if (obj == null) continue;
            result.add(new Option(
                    obj.optString("value", ""),
                    obj.optString("label", obj.optString("value", "")),
                    obj.optString("description", ""),
                    obj.optString("key", ""),
                    obj.optBoolean("disabled", false),
                    obj.optString("current_value", ""),
                    obj.has("current_value")));
        }
        return result;
    }

    private static List<Condition> parseConditions(JSONArray array) {
        if (array == null) return Collections.emptyList();
        List<Condition> result = new ArrayList<>();
        for (int i = 0; i < array.length(); i++) {
            JSONObject obj = array.optJSONObject(i);
            if (obj == null) continue;
            String key = obj.optString("key", "");
            if (key.isEmpty()) continue;
            result.add(new Condition(key, obj.optString("op", "equals"), obj.optString("value", "")));
        }
        return result;
    }
}
