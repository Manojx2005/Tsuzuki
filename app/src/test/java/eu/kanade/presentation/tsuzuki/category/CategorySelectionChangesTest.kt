package eu.kanade.presentation.tsuzuki.category

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import tachiyomi.core.common.preference.CheckboxState
import tachiyomi.domain.category.model.Category

class CategorySelectionChangesTest {
    @Test
    fun `reports only changed checkboxes`() {
        val selected = category(1)
        val unselected = category(2)
        val unchanged = category(3)

        categorySelectionChanges(
            initialSelection = listOf(
                CheckboxState.State.Checked(selected),
                CheckboxState.State.None(unselected),
                CheckboxState.State.Checked(unchanged),
            ),
            selection = listOf(
                CheckboxState.State.None(selected),
                CheckboxState.State.Checked(unselected),
                CheckboxState.State.Checked(unchanged),
            ),
        ) shouldBe (listOf(2L) to listOf(1L))
    }

    @Test
    fun `distinguishes including excluding and leaving mixed categories alone`() {
        val include = category(1)
        val exclude = category(2)
        val unchanged = category(3)

        categorySelectionChanges(
            initialSelection = listOf(include, exclude, unchanged).map { CheckboxState.TriState.None(it) },
            selection = listOf(
                CheckboxState.TriState.Include(include),
                CheckboxState.TriState.Exclude(exclude),
                CheckboxState.TriState.None(unchanged),
            ),
        ) shouldBe (listOf(1L) to listOf(2L))
    }

    @Test
    fun `leaves an empty selection unchanged`() {
        categorySelectionChanges(emptyList(), emptyList()) shouldBe (emptyList<Long>() to emptyList())
    }

    private fun category(id: Long) = Category(id = id, name = "Category $id", order = id, flags = 0)
}
