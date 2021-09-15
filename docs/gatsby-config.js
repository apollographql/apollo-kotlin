const themeOptions = require('gatsby-theme-apollo-docs/theme-options');

module.exports = {
  plugins: [
    {
      resolve: 'gatsby-theme-apollo-docs',
      options: {
        ...themeOptions,
        root: __dirname,
        pathPrefix: '/docs/android',
        algoliaIndexName: 'android',
        subtitle: 'Client (Android)',
        description: 'A guide to using Apollo with Android',
        githubRepo: 'apollographql/apollo-android',
        defaultVersion: '2',
        versions: {
          '3': 'dev-3.x',
        },
        sidebarCategories: {
          null: [
            'index',
            'essentials/get-started-kotlin',
            'essentials/get-started-java',
            'essentials/get-started-multiplatform',
          ],
          'Tutorial': [
            'tutorial/00-introduction',
            'tutorial/01-configure-project',
            'tutorial/02-add-the-graphql-schema',
            'tutorial/03-write-your-first-query',
            'tutorial/04-execute-the-query',
            'tutorial/05-connect-queries-to-your-ui',
            'tutorial/06-add-more-info',
            'tutorial/07-paginate-results',
            'tutorial/08-add-a-details-view',
            'tutorial/09-write-your-first-mutation',
            'tutorial/10-authenticate-your-queries',
            'tutorial/11-subscriptions',
          ],
          'Fetching data': [
            'essentials/queries',
            'essentials/mutations',
            'essentials/normalized-cache',
            'essentials/http-cache',
            'advanced/persisted-queries',
            'essentials/using-aliases',
            'advanced/multi-modules',
            'advanced/client-awareness'
          ],
          'Languages & Extensions': [
            'advanced/coroutines',
            'advanced/rxjava2',
            'advanced/rxjava3',
            'advanced/reactor',
            'advanced/mutiny'
          ],
          Reference: [
            'essentials/plugin-configuration',
            'advanced/ui-tests',
            'essentials/custom-scalar-types',
            'advanced/no-runtime',
            'essentials/fragments',
            'essentials/inline-fragments',
            'essentials/migration',
          ],
        }
      }
    }
  ]
};
