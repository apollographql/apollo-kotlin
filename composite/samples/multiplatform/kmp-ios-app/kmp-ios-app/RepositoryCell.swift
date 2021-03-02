//
//  RepositoryView.swift
//  kmp-ios-app
//
//  Created by Ellen Shapiro on 4/24/20.
//

import SwiftUI
import kmp_lib_sample

struct RepositoryCell: View {
    let repo: RepositoryFragment
    
    var body: some View {
        VStack(alignment: .leading) {
            HStack {
                Text(repo.name)
                    .bold()
            }
            Text(repo.repoDescription ?? "")
                .italic()
        }
    }
}

struct RepositoryCell_Previews: PreviewProvider {
    static var previews: some View {
        let sampleRepo = RepositoryFragmentImpl.Data(__typename: "Repository", id: "1", name: "TestRepo", repoDescription: "Repo Description")
        return RepositoryCell(repo: sampleRepo)
    }
}


