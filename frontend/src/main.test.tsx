const renderMock = jest.fn();
const createRootMock = jest.fn(() => ({ render: renderMock }));

jest.mock('react-dom/client', () => ({
  createRoot: createRootMock,
}));

jest.mock('./App', () => ({
  __esModule: true,
  default: () => null,
}));

document.body.innerHTML = '<div id="root"></div>';
import './main';

/** Tests for frontend bootstrap entrypoint. */
describe('main bootstrap', () => {
  /**
   * main.tsx mounts the app through ReactDOM.createRoot.
   *
   * <pre>
   * Theme: Bootstrap
   * Test view: main.tsx mounts app into #root
   * Test conditions: entry module is imported
   * Test result: createRoot and render are called once
   * </pre>
   */
  it('mounts app into root element', () => {
    expect(createRootMock).toHaveBeenCalledWith(document.getElementById('root'));
    expect(renderMock).toHaveBeenCalledTimes(1);
  });
});
