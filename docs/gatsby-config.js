const themeOptions = require('gatsby-theme-apollo-docs/theme-options');

module.exports = {
  pathPrefix: '/docs/android',
  plugins: [
    {
      resolve: 'gatsby-theme-apollo-docs',
      options: {
        ...themeOptions,
        root: __dirname,
        subtitle: 'Apollo Android Guide',
        description: 'A guide to using Apollo with Android',
        githubRepo: 'apollographql/apollo-android',
        sidebarCategories: {
          null: [
            'index'
          ],
          Essentials: [
            'essentials/get-started',
            'essentials/queries',
            'essentials/mutations'
          ]
        }
      }
    }
  ]
};
