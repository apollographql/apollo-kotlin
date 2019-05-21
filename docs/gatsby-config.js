module.exports = {
  __experimentalThemes: [
    {
      resolve: 'gatsby-theme-apollo-docs',
      options: {
        root: __dirname,
        subtitle: 'Apollo Android Guide',
        description: 'A guide to using Apollo with Android',
        contentDir: 'docs/source',
        basePath: '/docs/android',
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
