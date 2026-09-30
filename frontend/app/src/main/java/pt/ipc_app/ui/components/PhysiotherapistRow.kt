package pt.ipc_app.ui.components

import pt.ipc_app.R

import androidx.compose.ui.res.stringResource

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import pt.ipc_app.service.models.users.PhysiotherapistOutput
import pt.ipc_app.service.models.users.Rating
import java.util.*

@Composable
fun PhysiotherapistRow(
    physiotherapist: PhysiotherapistOutput? = null,
    onPhysiotherapistClick: () -> Unit = { }
) {

    Column(
        verticalArrangement = Arrangement.Center,
        modifier = Modifier
            .width(300.dp)
            .height(80.dp)
            .background(Color.White)
            .border(1.dp, Color(204, 202, 202, 255))
            .clickable {
                onPhysiotherapistClick()
            }
            .padding(8.dp)
    ) {

        if (physiotherapist != null)
            PhysiotherapistInfo(physiotherapist = physiotherapist)
        else {
            Row {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = stringResource(R.string.ui_search),
                    modifier = Modifier.padding(end = 4.dp)
                )
                Text(stringResource(R.string.ui_search_for_physiotherapists_to_connect))
            }
        }
    }
}

@Composable
fun PhysiotherapistInfo(
    physiotherapist: PhysiotherapistOutput
) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center
    ) {
        Text(physiotherapist.name)
        PhysiotherapistRating(rating = physiotherapist.rating)
    }
}

@Composable
fun PhysiotherapistRating(
    rating: Rating
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(Icons.Default.Star, contentDescription = null,
            tint = pt.ipc_app.ui.theme.MediumBlue, modifier = Modifier.padding(end = 6.dp))
        val average = String.format(java.util.Locale.getDefault(), "%.1f", rating.averageStarts)
        Text(stringResource(R.string.physiotherapist_review_summary, average, rating.nrOfReviews),
            style = MaterialTheme.typography.body2, color = pt.ipc_app.ui.theme.DarkGrey)
    }
}

@Preview
@Composable
fun PhysiotherapistRowWithoutPhysiotherapistPreview() {
    PhysiotherapistRow()
}

@Preview
@Composable
fun PhysiotherapistRowPreview() {
    PhysiotherapistRow(
        physiotherapist = PhysiotherapistOutput(UUID.randomUUID(), "Miguel", "miguel@gmail.com", Rating(4.8F, 3))
    )
}