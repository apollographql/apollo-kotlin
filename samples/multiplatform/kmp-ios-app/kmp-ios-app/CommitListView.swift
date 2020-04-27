//
//  CommitListView.swift
//  kmp-ios-app
//
//  Created by Ellen Shapiro on 4/26/20.
//

import kmp_lib_sample
import SwiftUI

struct CommitListView: View {
    let repoManager: RepositoryManager
    
    var body: some View {
        Text("Coming soon!")
    }
}

struct CommitListView_Previews: PreviewProvider {
    static var previews: some View {
        let manager = RepositoryManager()
        return CommitListView(repoManager: manager)
    }
}
