import com.ry.useful.old.database.Column;
import com.ry.useful.old.database.SQLiteResultMap;

/**
 * Java class created on 06/04/2022 for usage in project FunctionalUtils.
 *
 * @author -Ry
 */
@SQLiteResultMap(isOverrideJvm = true)
public class TestSQLite {

    private String name;

    @Column("name")
    private void setName(String s) {
        this.name = s;
    }
}
