# ML Lab Web App
This folder container the ML Lab web app which is build with JavaScript and React.
It communicates with the ML Lab backend (via the contaxy API) and provides the overall UI structure of ML Lab.
The ML Lab components are integrated into this UI via iframes.

## Develop
During development, it can be very helpful to run the web app locally without building the production bundle and putting it into the ML Lab backend docker image.
To do this, execute `yarn run start-debug` and open [http://localhost:3000](http://localhost:3000) in your browser.
Note that this debug server will automatically reload if the web application is changed.
To test the integration with the ML Lab backend, this debug web app can be connected to any running ML Lab instance.
By default, the ML Lab backend is expected to run at http://localhost:30010/ which is the default when starting ML Lab with docker compose.
In case you want to connect to a different ML Lab instance, adjust the package.json file accordingly.

This project uses [React](https://reactjs.org) as the main framework. Components should be written as [React Hooks](https://reactjs.org/docs/hooks-intro.html) instead of the old class-style wherever possible. See [Section Components Guide](#components-guide) for some more guidelines. For component styling this project uses the [styled-components library](https://github.com/styled-components/styled-components) (see [Section Component Styling](#component-styling)). As the default design we use [Material](https://material.io/design) and, hence, the [Material-UI Library](https://material-ui.com).

For code styling, [eslint](https://eslint.org) is used for linting and [prettier](https://prettier.io) is used for formatting (see [this page](https://prettier.io/docs/en/comparison.html) for learning about linting vs. formatting). The linting rules are listed in [.eslintrc.json](./eslintrc.json). The configuration for prettier can be found in [.prettierrc](./.prettierrc). The configurations adhere to [Airbnb's JavaScript style guide](https://github.com/airbnb/javascript). For CSS styling, [stylelint](https://stylelint.io) is used for which the configuration can be found in the [.stylelintrc.json](./.stylelintrc.json) file.

Docs should be written in [JSDoc](https://jsdoc.app/about-getting-started.html) format, though overall we advocate self-explanatory code over comments.

It was bootstrapped with [Create React App](https://github.com/facebook/create-react-app) (`yarn create react-app react-webapp`) and, thus, uses the pre-configured webpack and babel build tools.

The used package manager for installing packages is [yarn](https://classic.yarnpkg.com/en/docs/install/#mac-stable).

### Code Style

When contributing code, please try to make the code following the project's codestyle setup as described in the [Development summary section](#develop). We recommend to use plugins in your IDE to already keep an eye on the style while developing.
After executing `yarn install` as they are defined in the [./package.json](./package.json), you can run style checking like following:

- Formatting:
  - `yarn run prettier <path-to-file>`: this command formats the file and saves it.
- Linting (shows the problems but does not fix them):
  - `python build.py --check` runs all checks as defined in the [build.py](./build.py) or manually:
  - `yarn run lint:js`: checks the JavaScript files (files ending with `.js`/`.jsx`).
  - `yarn run lint:css`: checks the `.css` files.

Sometimes, you have to do something that is not allowed by the linting rules. For example, property spreading in React makes sense sometimes. In this example, you can disable the linter for the specific line by adding `// eslint-disable-line react/jsx-props-no-spreading`. Instead of disabling a rule globally, this forces you to think about your decision instead of allowing slopiness by default.

### Production Build

Execute `python build.py --make` to build the app. Under the hood, it uses `yarn build` for production to the `build` folder.\
It correctly bundles React in production mode and optimizes the build for the best performance.

The build is minified and the filenames include the hashes.\
Your app is ready to be deployed!

See the section about [deployment](https://facebook.github.io/create-react-app/docs/deployment) for more information.

### Development Walkthrough

> Add some information here about the structure of the app, what the entrypoint is, and what tools are used.

#### Project Structure

The project uses follwing structure:

- `src/`: Contains the source code of the web app, for example the React components. Development usually happens in here. The `/src/index.jsx` is the entry point into the application.
- `public/`: Contains the default public resources that must be available such as the `index.html`. It also contains the `locales/` directory for translation files other than English; see the [Internationalization Section](#internationalization) for more information.
- `build/`: The generated directory that contains the bundled web app. This folder is ignored by `git` and should not be pushed
- `node_modules/`: The installed packages. This folder is ignored by `git` and should not be pushed.

Inside of the `src/` folder, there should be following structure (inspired by this [blog post](https://www.devaradise.com/react-project-folder-structure)):

- `components/`: Capsulated components appear here. Artifacts that are specific to a component should be packaged together, for example `components/dashboard/` should have all components and styles and images that are just relevant for the `dashboard` component (domain-based structure).
- `pages/`: Reflects the routes of the application and is composed of different components. Each component in this folder should have its own route. If a page has specific components that are only used within that page (especially those which are composed of more general components), you can add them in a sub-directory here instead of the `components/` directory.
- `utils/`: Functionality that is generally relevant for your application and could be used in multiple places.
- `app/`: Contains app essentials such as routes and the store in form of `store.js` [when using Redux](https://redux.js.org/tutorials/essentials/part-1-overview-concepts).
- `services/`: Contains JavaScript functions and clients that manage API integrations.
- `assets/`: Should contain style and images and other resources that are generally relevant for your application and not only for a specific component.
- `stories/`: Contains general Storybook files such as introduction and assets that are not directly linked to a specific component. It should not contain the actual component stories.

If files belong together, for example a component has a related `.stories.jsx` and `.test.jsx` file, put them into an extra folder. Also, when needed add an `index.js` file to a component directory to make it easier to import as shown [here](src/pages/App/index.js).

Add Storybook files (see the [Storybook Section](#storybook)) next to the components they describe. Story files must follow the `<component-name>.stories.jsx` name pattern. For example, if you have a component `src/dashboard/Dashboard.jsx`, put the stories file under `src/dashboard/Dashboard.stories.jsx`.

Add test files next to the code they are testing (see the [Testing Section](#testing)). Test files must follow the `<component-name>.test.jsx` name pattern.

#### Component Styling

You can find an example of how [styled-components](https://github.com/styled-components/styled-components) can be used [here](./src/components/Button.jsx#L5). With the stated [motivation](https://styled-components.com/docs/basics#motivation) of the library, the style of a component should be bundled with the component and, usually, lies in the same file.

#### Components Guide

##### Arrow Functions

Inside of functional components, we prefer [arrow functions](https://developer.mozilla.org/en-US/docs/Web/JavaScript/Reference/Functions/Arrow_functions) `const foo = () => {}` over function declarations `function foo(){}`.

##### Memoized Functions

In class components, arrow functions should be defined outside of the `render` function to avoid [any performance problems](https://reactjs.org/docs/faq-functions.html#arrow-function-in-render). For functional components, define non-component specific functions outside of the component function or use `useMemo`/`useCallback` functions to avoid re-creation of functions (see [this example](https://stackoverflow.com/a/56477027/5379273) and the [React documentation](https://reactjs.org/docs/hooks-reference.html#usememo)).

##### Memoized Components

When your component uses components that render often and always with the same props, consider using a memoized version of those components via `React.memo` to avoid unnecessary re-renders (check out [this blogpost](https://dmitripavlutin.com/use-react-memo-wisely/)).

#### Internationalization

We use the [react-i18next](https://react.i18next.com) ([GitHub](https://github.com/i18next/react-i18next)) library for translations. The translation file for languages other than English is loaded dynamically upon need via the [i18next-http-backend](https://github.com/i18next/i18next-http-backend) library. The translation files are placed under [./public/locales](./public/locales). Add a new folder there for extending the languages or edit the languages there to add new translations. The [English translation](./src/en.json) file is pre-bundled with the app which is why it is placed under the `src/` directory.
You can find an example of how to use the `i18next` in [./src/pages/App/App.jsx](./src/pages/App/App.jsx#L11).

#### Storybook

When we want to document our components further than using JSDoc, we use [Storybook](https://storybook.js.org/docs/react/get-started/introduction). In the [Stories directory](./src/stories/) you can find some example stories (this project was initialized via `npx sb init`).  
The Storybook server can be started via `yarn run storybook`.

#### Testing

This project uses [Jest](https://create-react-app.dev/docs/running-tests) and [react-testing-library](https://github.com/testing-library/react-testing-library) for testing as it comes pre-bundled with _Create React App_.
The official Jest documentation recommends to add at least smoke tests to make sure that components render ([source](https://create-react-app.dev/docs/running-tests#testing-components)), so we go along with this recommendation ;)
Use `test()` instead of it's alias `it()` ([source](https://jestjs.io/docs/en/api.html#testname-fn-timeout)). See the [App.test.jsx](./src/App.test.jsx) file for an example.

To run the tests, execute `yarn test`. To see test coverage, execute `yarn test -- --coverage` ([source](https://create-react-app.dev/docs/running-tests/#coverage-reporting)).
