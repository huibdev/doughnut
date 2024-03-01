import { describe, it, beforeEach, afterEach } from "vitest";
import NotebooksPage from "@/pages/NotebooksPage.vue";
import helper from "../helpers";
import makeMe from "../fixtures/makeMe";

helper.resetWithApiMock(beforeEach, afterEach);

describe("Notebooks Page", () => {
  it("fetch API to be called ONCE", async () => {
    const notebook = makeMe.aNotebook.please();

    helper.managedApi.restNotebookController.myNotebooks = vi
      .fn()
      .mockResolvedValue({
        notebooks: [notebook],
        subscriptions: [],
      });
    helper.component(NotebooksPage).withStorageProps({}).render();
    expect(
      helper.managedApi.restNotebookController.myNotebooks,
    ).toBeCalledTimes(1);
  });
});
