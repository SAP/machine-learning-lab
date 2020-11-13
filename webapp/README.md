# ML Lab Webapp

The ML Lab Webapp gives access to a variety of information and tools needed for an efficient process with regards to data science and software engineering. It allows you to create a project, upload datasets, see experiment results, get trained models, and deploy them to production with a single click.

## Usage

To use the ML Lab, please refer to [this guide](../README.md#usage).

## Develop

This project uses [React](https://reactjs.org) as the main framework. The project was started before the new [React Hooks](https://reactjs.org/docs/hooks-intro.html) existed. Hence, existing components are written using the class style. However, new components should be written as [React Hooks](https://reactjs.org/docs/hooks-intro.html) instead of the old class-style wherever possible. As the default design we use [Material](https://material.io/design) and, hence, the [Material-UI Library](https://material-ui.com).

For code styling, [eslint](https://eslint.org) is used for linting and [prettier](https://prettier.io) is used for formatting (see [this page](https://prettier.io/docs/en/comparison.html) for learning about linting vs. formatting). The linting rules are listed in [.eslintrc.js](./eslintrc.js). The configuration for prettier can be found in [.prettierrc](./prettierrc). The configurations adhere to [Airbnb's JavaScript style guide](https://github.com/airbnb/javascript). For CSS styling, [stylelint](https://stylelint.io) is used for which the configuration can be found in the [.stylelintrc.json](./.stylelintrc.json) file.
> Remark: The codestyle rules were added to the project only after some time into the making. As a result, existing code might not follow them. For newly added code, though, please make sure to follow the guidelines described in the [Development summary section](#develop) to increase code quality over time :)

Docs should be written in [JSDoc](https://jsdoc.app/about-getting-started.html) format, though overall we advocate self-explanatory code over comments.

It was bootstrapped with [Create React App](https://github.com/facebook/create-react-app) (`yarn create react-app react-webapp`) and, thus, uses the pre-configured webpack and babel build tools.

The used package manager for installing packages is [npm].

> All `npm` commands in this documentation can be executed via `npm run container` to run it inside of the development container instead of using the host. Hereby, the current directory is mounted into the container. See the `npm run container` command in the [package.json](./package.json).

### Generate ML Lab Client

When executing the parent's [build.py](../build.py), the JavaScript client is automatically generated. If you want to do it manually, execute the following command:

1. Download the [Swagger Codegen Jar](https://repo1.maven.org/maven2/io/swagger/codegen/v3/swagger-codegen-cli/3.0.23/swagger-codegen-cli-3.0.23.jar)
2. Execute `java -jar swagger-codegen-cli.jar generate -i ./backend/lab-service/src/main/resources/swagger/swagger.json -l javascript -o ./temp-client --additional-properties useES6=true`
3. Copy the files `./temp-client/src` to `./webapp/src/services/client`
4. Rename generated `index.js` to `lab-api.js`.

Alternatively to the Java Client, the online [Swagger Editor](https://editor.swagger.io/) could be used.

### Code Style

> The codestyle rules were added to the project only after some time into the making. As a result, existing code might not follow them. For newly added code, though, please make sure to follow the guidelines described in the [Development summary section](#develop) to increase code quality over time :)

We recommend to use plugins in your IDE to already keep an eye on the style while developing.
After executing `npm install` as they are defined in the [./package.json](./package.json), you can run style checking like following:

- Formatting:
  - `npm run prettier <path-to-file>`: this command formats the file and saves it.
- Linting (shows the problems but does not fix them):
  - `python build.py --check` runs all checks as defined in the [build.py](./build.py) or manually:
  - `npm run lint:js`: checks the JavaScript files (files ending with `.js`/`.jsx`).
  - `npm run lint:css`: checks the `.css` files.

Sometimes, you have to do something that is not allowed by the linting rules. For example, property spreading in React makes sense sometimes. In this example, you can disable the linter for the specific line by adding `// eslint-disable-line react/jsx-props-no-spreading`. Instead of disabling a rule globally, this forces you to think about your decision instead of allowing slopiness by default.

### Build

Execute this command in the project root folder to build this project (only needed if npm packages are needed. For code changes, python.run is sufficient as react makes on-the-fly builds):

```bash
python build.py --make
```

This script compiles the project and builds the static version of the wepapp. For additional script options:

```bash
python build.py --help
```

### Run

Execute this command in the project root folder to start a local :

```bash
npm run start
```

__Local Testing with Remote Endpoint__  
The webapp will use the endpoint relative to the current host. Hence, if the ML Lab is accessed via http://localhost:8090, it calls the endpoint via http://localhost:8090/api. If it is accessed via http://10.20.30.40:30000, the endpoint http://10.20.30.40:30000/api is used.  
If you want to use an ML Lab endpoint different to the webapp host, edit the `REACT_APP_LAB_ENDPOINT` variable in the `start` command in the [package.json](./package.json).

Visit http://localhost:8090/app to access the webapp.

### Deploy

To use the ML Lab, please refer to [this guide](../README.md#deploy).

## Dev Walkthrough

Last Edited: April 27 2018, 16:19 +0000
Project: ML Lab
Tags: Technical Note

This guide should you lead though the most important parts of the ML Lab interface. We will cover same basic example so that you understand the relations between the different parts.

### Architecture

This section should you give an overview of the main architecture (folder design) of the ML Lab:

- > build

  React apps have the advantage that you can develop and test them pretty fast, but also deploy them in a optimised version. Therefore, the build folder is the bucket to store the optimised part. (not important for development)

- node_modules

  This Folder includes all necessary third-party libraries to run the app. (please do not change something in here manually)

- > public

  This Folder includes the basic files of the application for example the html file and favicons.

  - `index.html`
- src

  This folder contains the visualisation and it's functional part of the ML Lab. 

  - components

    Small fragments of code (e.g. table) of the application that are used in several parts.

  - css

    You can specify your own style for parts that appear in several places inside the application.

  - scenes

    Scenes are the pages of our application (e.g. Dashboard, Datasets,...). 

    - `<scence>.js`
    - components

      Fragments that are only used in one scene, but where excluded from the main .js-file  due to simplification.

  - services
    - client

      Contains the files to communicate with the backend (e.g getProjects, DeleteFile, ...)

      - `ml-lab-api.js`
    - handler

      These files deal with the communication between the components (reduxUtils), their formation (parser) and constant parameters (constants, e.g. widget appearance)

      - `constants.js`
      - `parser.js`
      - `reduxUtils.js`
  - `index.js`

### Quick Start of the Application

After the application is running on the server you can access the interface under the following endpoint:

> <endpoint>:<port>/app

![](https://www.notion.so/file/https%3A%2F%2Fs3-us-west-2.amazonaws.com%2Fsecure.notion-static.com%2F0d6face0-1730-4d0f-a138-46bccaa65e41%2FArchitecture.png)

- Index

  The server will first execute the `index.html` and `index.js` files. The `index.js` takes care of

  - Initialising the ReduxStore
  - Register ServiceWorker
  - Initialise the HashRouter
  - Use the CookieStore
  - Render the App Component
- App

  The `App.js` is the central instance in the application. It renders the wrappers for all following scenes (pages).

  - Toolbar
    - Drawer with Navigation Links
    - Create new Project Button
    - Project Selector
  - Content Container
    - Router to all possible scenes

          <Switch>
                      <Route path="/" exact component={Dashboard}/>
                      <Route path="/datasets" exact component={Datasets}/>
          						/*...*/
          </Switch>

### Scenes

The call the different pages of our application **scenes**. They are saved in the scenes folder with the following structure:

- Dashboard
- Datasets
- Workspace
- Experiments
- Models
- Services
- Jobs
- Administration (Portainer)

### Exchange Environment

#### Redux

Redux is an event-based parameter exchange tool, which allows information sharing though the entire architecture. The ML Lab shares the status of the current project though out the application. If the project was changed other components can react on this change, for example by loading the datasets for the new project.

- Connect to ReduxStore

  To be part of the redux network you just have to add the following line in the end of your component.

      export default connect(ReduxUtils.mapStateToProps)(<component_name>)
      //recieve the changes
      
      export default connect(ReduxUtils.mapStateToProps), ReduxUtils.mapDispatchToProps (<component_name>)
      //receive and send changes

  Fill the function `componentWillReceiveProps(nextProps)` with your desired reaction on the receiving information.

- Possible Statements and Access

      //Possible Statements
      onInpChange({target:{value: <project>}})
      
      onCreateProject(<project>)
      
      onNoProjectAvailable(<project>)
      
      onProjectDelet(<project>)

      //Access Statements
      this.props.<statement>

- FlowDiagram: Project Selection

  ![](https://www.notion.so/file/https%3A%2F%2Fs3-us-west-2.amazonaws.com%2Fsecure.notion-static.com%2F60dcb777-60bc-47d5-9374-9b1a5de4c4d1%2FChangeProject.png)

  This Flow Diagram shows the process of determine the current project of ML Lab. After the user entered the application though the entry point the `ProjectSelector.js` is calling though the `ml-lab-api.js` the server for all existing projects (for simplification I will leave the possibility of no existing projects away).

  Afterwards, the component is accessing the cookies of the ML Lab to find out if the ML Lab saved a project in the session.

  - Yes

    Use the cookie project for the current project.

  - No

    Use the first project in the list of the returned projects from the server.

  The ProjectSelector is calling `this.props.onInpChange({target: {value: <project_name>}}` to exchange the information of the new current project. This call will execute all components that are connected to the ReduxStore (see Connect to ReduxStore). They can individually handle their reaction (for example Dashboard: Change the highlighting for the current project). 

  The same procedure will happen if the user is changing the current project except that the project will be saved in the cookies.

  ![](https://www.notion.so/file/https%3A%2F%2Fs3-us-west-2.amazonaws.com%2Fsecure.notion-static.com%2F47fadc4b-3b83-4272-bd25-bbf4313b9ea5%2FReduxStore.png)

#### Cookies

The ML Lab is using **react-cookie** to save information of the application over the session. With the following code lines cookies are activated in your component:

    import { withCookies, Cookies } from 'react-cookie';
    
    //...
    
    //Get parameter of cookie
    cookies.get(<parameter_name>, Constants.COOKIES.options);
    
    //Set parameter of cookie
    cookies.set(<parameter_name>,<new_value>,Constants.COOKIES.options);
    
    //...
    
    //Use Cookies and Redux together
    export default withCookies(connect(ReduxUtils.mapStateToProps, ReduxUtils.mapDispatchToProps)(<Component>));
    
    //Just Cookies
    export default withCookies(<Component>);

### Testing

This project uses [Jest](https://create-react-app.dev/docs/running-tests) and [react-testing-library](https://github.com/testing-library/react-testing-library) for testing as it comes pre-bundled with _Create React App_.
The official Jest documentation recommends to add at least smoke tests to make sure that components render ([source](https://create-react-app.dev/docs/running-tests#testing-components)), so even though test coverage is really small for this project so far, consider writing a small smoke test for contributed components :)
If you write tests, use `test()` instead of it's alias `it()` ([source](https://jestjs.io/docs/en/api.html#testname-fn-timeout)). See the [App.test.jsx](./src/App.test.jsx) file for an example.

To run the tests, execute `npm run test`. To see test coverage, execute `npm run test -- --coverage` ([source](https://create-react-app.dev/docs/running-tests/#coverage-reporting)).
