package xin.vanilla.banira.editable;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

/**
 * Banira 自定义配置编辑器与网络同步使用的配置视图；实现通常由 {@link AutoConfigEditableHolder} 提供。
 */
public interface EditableConfigHolder {

    String getModId();

    String getConfigName();

    /**
     * 是否允许通过配置编辑器将修改同步至服务端（通常仅「公共」配置文件为 true）。
     */
    boolean canSyncToServer();

    void save();

    /**
     * 批量写入字段后调用，触发 {@link me.shedaniel.autoconfig.ConfigData#validatePostLoad()}。
     */
    void validateAfterChanges();

    <T> T get(String path);

    void set(String path, Object value);

    @Nullable
    ConfigEntryDescriptor getDescriptor(String path);

    List<ConfigEntryDescriptor> getDescriptors();

    @Nullable
    ConfigCategoryTitleSpec getCategoryTitleSpec(String categoryPath);

    List<CategoryTreeNode> getCategoryTree();

    // region 分类树

    final class CategoryTreeNode {
        private final String categoryPath;
        private final String displayName;
        private final List<ConfigEntryDescriptor> entries;
        private final List<CategoryTreeNode> children = new ArrayList<>();

        public CategoryTreeNode(String categoryPath, String displayName, List<ConfigEntryDescriptor> entries) {
            this.categoryPath = categoryPath;
            this.displayName = displayName;
            this.entries = List.copyOf(entries);
        }

        public String getCategoryPath() {
            return categoryPath;
        }

        public String getDisplayName() {
            return displayName;
        }

        public List<ConfigEntryDescriptor> getEntries() {
            return entries;
        }

        public List<CategoryTreeNode> getChildren() {
            return children;
        }

        public void addChild(CategoryTreeNode child) {
            children.add(child);
        }
    }

    // endregion 分类树
}
