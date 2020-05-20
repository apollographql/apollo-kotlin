const themeOptions = require('gatsby-theme-apollo-docs/theme-options');

module.exports = {
  pathPrefix: '/docs/android',
  plugins: [
    {
      resolve: 'gatsby-theme-apollo-docs',
      options: {
        ...themeOptions,
        root: __dirname,
        subtitle: 'Client (Android)',
        description: 'A guide to using Apollo with Android',
        githubRepo: 'apollographql/apollo-android',
        sidebarCategories: {
          null: [
            'index',
            'essentials/get-started',
          ],
          'Fetching data': [
            'essentials/queries',
            'essentials/mutations',
            'essentials/caching',
            'advanced/persisted-queries',
          ],
          'Languages & Extensions': [
            'advanced/coroutines',
            'advanced/multiplatform',
            'advanced/rxjava2',
          ],
          Reference: [
            'essentials/plugin-configuration',
            'advanced/android',
            'essentials/migration',
            'advanced/no-runtime',
          ],
        }
      }
    }
  ]
};
